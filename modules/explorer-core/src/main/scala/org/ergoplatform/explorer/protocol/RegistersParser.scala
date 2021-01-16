package org.ergoplatform.explorer.protocol

import cats.MonadError
import cats.syntax.flatMap._
import org.ergoplatform.explorer.HexString
import org.ergoplatform.explorer.protocol.models.RegisterValue
import sigmastate.SType
import sigmastate.Values.EvaluatedValue
import sigmastate.serialization.ValueSerializer

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
          case v: EvaluatedValue[_] => F.pure(RegisterValue(v.tpe.toTermString, v.value.toString))
          case v                    => F.raiseError(new Exception(s"Got non constant value [$v] in register"))
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
}
