package org.ergoplatform.explorer.protocol

import cats.MonadError
import cats.instances.list._
import cats.syntax.traverse._
import org.ergoplatform.explorer.protocol.models.RegisterValue
import org.ergoplatform.explorer.{HexString, SigmaType}
import scorex.util.encode.Base16
import sigmastate.Values.{Constant, ConstantNode, EvaluatedValue, SigmaPropConstant}
import sigmastate.basics.DLogProtocol.ProveDlogProp
import sigmastate.serialization.ValueSerializer
import sigmastate.{SGroupElement, _}
import tofu.syntax.monadic._
import tofu.syntax.raise._

import scala.reflect.ClassTag

trait RegistersParser[F[_]] {

  def parseAny(raw: HexString): F[RegisterValue]

  def parse[T <: SType](raw: HexString)(implicit ev: ClassTag[T#WrappedType]): F[T#WrappedType]
}

object RegistersParser {

  def apply[F[_]](implicit F: MonadError[F, Throwable]): RegistersParser[F] =
    new RegistersParser[F] {

      def parseAny(raw: HexString): F[RegisterValue] =
        F.catchNonFatal(ValueSerializer.deserialize(raw.bytes)).flatMap {
          case v: EvaluatedValue[_] =>
            renderEvaluatedValue(v)
              .map { case (tp, vl) => RegisterValue(tp, vl) }
              .orRaise[F](new Exception(s"Failed to render constant value [$v] in register"))
          case v => F.raiseError(new Exception(s"Got non constant value [$v] in register"))
        }

      def parse[T <: SType](raw: HexString)(implicit ev: ClassTag[T#WrappedType]): F[T#WrappedType] =
        F.catchNonFatal(ValueSerializer.deserialize(raw.bytes)).flatMap {
          case v: EvaluatedValue[_] =>
            v.value match {
              case wrappedValue: T#WrappedType => F.pure(wrappedValue)
              case wrappedValue =>
                F.raiseError(new Exception(s"Got wrapped value [$wrappedValue] of unexpected type in register"))
            }
          case v => F.raiseError(new Exception(s"Got non constant value [$v] in register"))
        }
    }

  private def renderEvaluatedValue[T <: SType](value: EvaluatedValue[T]): Option[(SigmaType, String)] =
    value.tpe match {
      case SSigmaProp | SGroupElement =>
        value match {
          case SigmaPropConstant(ProveDlogProp(dlog)) =>
            Some(SigmaType.SimpleKindSigmaType.SSigmaProp -> Base16.encode(dlog.pkBytes))
          case ConstantNode(groupElem, SGroupElement) =>
            Some(
              SigmaType.SimpleKindSigmaType.SGroupElement ->
              Base16.encode(groupElem.asInstanceOf[SGroupElement.type#WrappedType].getEncoded.toArray)
            )
          case _ => None
        }
      case prim: SPrimType =>
        val typeTerm = prim.toString.replaceAll("\\$", "")
        SigmaType.parse(typeTerm).map(_ -> value.value.toString)
      case tuple: STuple =>
        val typeTerm = tuple.toString.replaceAll("\\$", "")
        SigmaType.parse(typeTerm).flatMap { tp =>
          val untypedElems = value.value match {
            case (a, b) => List(a, b)
            case _      => value.value.asInstanceOf[tuple.WrappedType].toArray.toList
          }
          val elems =
            untypedElems.zip(tuple.items).map { case (vl, tp) => Constant[SType](vl.asInstanceOf[tp.WrappedType], tp) }
          elems.traverse(e => renderEvaluatedValue(e).map(_._2)).map { xs =>
            tp -> ("(" + xs.mkString(",") + ")")
          }
        }
      case SCollectionType(SByte) =>
        Some(
          SigmaType.SCollection(SigmaType.SimpleKindSigmaType.SByte) ->
          Base16.encode(value.value.asInstanceOf[SCollection[SByte.type]#WrappedType].toArray)
        )
      case coll: SCollection[_] =>
        val typeTerm = coll.toString.replaceAll("\\$", "")
        SigmaType.parse(typeTerm).flatMap { tp =>
          val elems = value.value.asInstanceOf[coll.WrappedType].toArray.toList.map(Constant(_, coll.elemType))
          elems.traverse(e => renderEvaluatedValue(e).map(_._2)).map { xs =>
            tp -> ("[" + xs.mkString(",") + "]")
          }
        }
      case option: SOption[_] =>
        SigmaType.parse(option.toTermString).flatMap { tp =>
          val elem = value.value.asInstanceOf[option.WrappedType].map(Constant(_, option.elemType))
          elem match {
            case Some(value) => renderEvaluatedValue(value).map(r => tp -> r._2)
            case None        => Some(tp -> "null")
          }
        }
    }
}
