package org.ergoplatform.explorer.protocol

import cats.data.OptionT
import cats.syntax.either._
import cats.{Applicative, Eval}
import mouse.any._
import org.ergoplatform.explorer._
import org.ergoplatform.{ErgoAddress, ErgoAddressEncoder, ErgoTreePredef, Pay2SAddress}
import scorex.crypto.hash.Sha256
import scorex.util.encode.Base16
import sigma.VersionContext
import sigma.ast.ErgoTree.ZeroHeader
import sigma.ast.{Constant, ConstantNode, DeserializationSigmaBuilder, ErgoTree, EvaluatedValue, SByte, SCollection, SGroupElement, SOption, SPrimType, SSigmaProp, STuple, SType, SigmaPropConstant}
import sigma.data.{CSigmaProp, ProveDlog}
import sigma.serialization.{ConstantSerializer, ErgoTreeSerializer, GroupElementSerializer, SigmaSerializer}
import tofu.Throws
import tofu.syntax.monadic._
import tofu.syntax.raise._

import scala.util.Try

object sigmaWrappers {

  private val treeVersion = VersionContext.V6SoftForkVersion // tree version for deserialization
  private val treeSerializer: ErgoTreeSerializer     = ErgoTreeSerializer.DefaultSerializer
  private val constantSerializer: ConstantSerializer = ConstantSerializer(DeserializationSigmaBuilder)

  @inline def deserializeErgoTree[F[_]: Applicative: Throws](raw: HexString): F[ErgoTree] = {
    VersionContext.withVersions(treeVersion, treeVersion) {
      Base16.decode(raw.unwrapped).map(treeSerializer.deserializeErgoTree).fold(_.raise, _.pure)
    }
  }

  @inline def extractErgoTreeConstants[F[_]: Applicative: Throws](
    raw: HexString
  ): F[List[(Int, Constant[SType], HexString)]] =
    deserializeErgoTree(raw).map {
      VersionContext.withVersions(treeVersion, treeVersion) {
        _.constants.zipWithIndex.toList.map { case (c, ix) =>
          val bw = SigmaSerializer.startWriter()
          constantSerializer.serialize(c, bw)
          val rawValue = HexString.fromStringUnsafe(Base16.encode(bw.toBytes))
          (ix, c, rawValue)
        }
      }
    }

@inline def deriveErgoTreeTemplateHash[F[_]: Applicative: Throws](ergoTree: HexString): F[ErgoTreeTemplateHash] =
  deserializeErgoTree(ergoTree).map { tree =>
    VersionContext.withVersions(treeVersion, treeVersion) {
      val rawBytes = Base16.decode(ergoTree.unwrapped).getOrElse(Array.emptyByteArray)
      val digest = Try(Sha256.hash(tree.template)).getOrElse(Sha256.hash(rawBytes)) // if tree cant' be parsed
      ErgoTreeTemplateHash.fromStringUnsafe(Base16.encode(digest))
    }
  }

  @inline def ergoTreeToAddress[F[_]: Applicative](
    ergoTree: HexString
  )(implicit enc: ErgoAddressEncoder): F[ErgoAddress] = {
    VersionContext.withVersions(treeVersion, treeVersion) {
      Base16
        .decode(ergoTree.unwrapped)
        .flatMap { bytes =>
          enc.fromProposition(treeSerializer.deserializeErgoTree(bytes))
        }
        .fold(_ => (Pay2SAddress(ErgoTreePredef.FalseProp(ZeroHeader)): ErgoAddress).pure, _.pure)
    }
  }

  @inline private def addressToErgoTree(
    address: Address
  )(implicit enc: ErgoAddressEncoder): ErgoTree =
    enc
      .fromString(address.unwrapped)
      .map(_.script)
      .get

  @inline def addressToErgoTreeHex(address: Address)(implicit enc: ErgoAddressEncoder): HexString =
    addressToErgoTree(address) |> (tree => HexString.fromStringUnsafe(Base16.encode(tree.bytes)))

  @inline def addressToErgoTreeNewtype(address: Address)(implicit enc: ErgoAddressEncoder): org.ergoplatform.explorer.ErgoTree =
    addressToErgoTreeHex(address) |> (tree => org.ergoplatform.explorer.ErgoTree(tree))

  import cats.instances.list._
  import cats.syntax.traverse._

  @inline def renderEvaluatedValue[T <: SType](ev: EvaluatedValue[T]): Option[(SigmaType, String)] = {
    def goRender[T0 <: SType](ev0: EvaluatedValue[T0]): OptionT[Eval, (SigmaType, String)] =
      ev0.tpe match {
        case SSigmaProp | SGroupElement =>
          ev0 match {
            case SigmaPropConstant(CSigmaProp(ProveDlog(dlog))) =>
              OptionT.some(SigmaType.SimpleKindSigmaType.SSigmaProp -> Base16.encode(GroupElementSerializer.toBytes(dlog)))
            case ConstantNode(groupElem, SGroupElement) =>
              OptionT.some(
                SigmaType.SimpleKindSigmaType.SGroupElement ->
                Base16.encode(groupElem.asInstanceOf[SGroupElement.WrappedType].getEncoded.toArray)
              )
            case _ => OptionT.none
          }
        case prim: SPrimType =>
          val typeTerm = prim.toString.replaceAll("\\$", "")
          OptionT.fromOption[Eval](SigmaType.parse(typeTerm)).map(_ -> ev0.value.toString)
        case tuple: STuple =>
          val typeTerm = tuple.toString.replaceAll("\\$", "")
          OptionT.fromOption[Eval](SigmaType.parse(typeTerm)).flatMap { tp =>
            val untypedElems = ev0.value match {
              case (a, b) => List(a, b)
              case _      => ev0.value.asInstanceOf[tuple.WrappedType].toArray.toList
            }
            val elems =
              untypedElems.zip(tuple.items).map { case (vl, tp) =>
                Constant[SType](vl.asInstanceOf[tp.WrappedType], tp)
              }
            elems.traverse(e => goRender(e).map(_._2)).map { xs =>
              tp -> ("[" + xs.mkString(",") + "]")
            }
          }
        case sigma.ast.SCollectionType(SByte) =>
          OptionT.some(
            SigmaType.SCollection(SigmaType.SimpleKindSigmaType.SByte) ->
            Base16.encode(ev0.value.asInstanceOf[SCollection[SByte.type]#WrappedType].toArray)
          )
        case coll: sigma.ast.SCollection[_] =>
          val typeTerm = coll.toString.replaceAll("\\$", "")
          OptionT.fromOption[Eval](SigmaType.parse(typeTerm)).flatMap { tp =>
            val elems = ev0.value.asInstanceOf[coll.WrappedType].toArray.toList.map(Constant(_, coll.elemType))
            elems.traverse(e => goRender(e).map(_._2)).map { xs =>
              tp -> ("[" + xs.mkString(",") + "]")
            }
          }
        case option: SOption[_] =>
          OptionT.fromOption[Eval](SigmaType.parse(option.toTermString)).flatMap { tp =>
            val elem = ev0.value.asInstanceOf[option.WrappedType].map(Constant(_, option.elemType))
            elem match {
              case Some(value) => OptionT(Eval.defer(goRender(value).value)).map(r => tp -> r._2)
              case None        => OptionT.some(tp -> "null")
            }
          }
      }
    goRender(ev).value.value
  }
}
