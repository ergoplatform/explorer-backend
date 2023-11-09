package org.ergoplatform.explorer.protocol

import cats.data.OptionT
import cats.syntax.either._
import cats.{Applicative, Eval, Monad}
import mouse.any._
import org.ergoplatform.explorer.Err.RequestProcessingErr.DexErr.ContractParsingErr.ErgoTreeSerializationErr.ErgoTreeDeserializationFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.DexErr.ContractParsingErr.{Base16DecodingFailed, ErgoTreeSerializationErr}
import org.ergoplatform.explorer._
import org.ergoplatform.{ErgoAddress, ErgoAddressEncoder, Pay2SAddress}
import scorex.crypto.hash.Sha256
import scorex.util.encode.Base16
import sigmastate.Values.{Constant, ConstantNode, ErgoTree, EvaluatedValue, FalseLeaf, SigmaPropConstant}
import sigmastate._
import sigmastate.basics.DLogProtocol.ProveDlogProp
import sigmastate.lang.DeserializationSigmaBuilder
import sigmastate.serialization.{ConstantSerializer, ConstantStore, ErgoTreeSerializer, SigmaSerializer}
import tofu.Throws
import tofu.syntax.monadic._
import tofu.syntax.raise._

import scala.util.Try

object sigma {

  private val treeSerializer: ErgoTreeSerializer     = ErgoTreeSerializer.DefaultSerializer
  private val constantSerializer: ConstantSerializer = ConstantSerializer(DeserializationSigmaBuilder)

  @inline def deserializeErgoTree[F[_]: Applicative: Throws](raw: HexString): F[Values.ErgoTree] =
    Base16
      .decode(raw.unwrapped)
      .map(treeSerializer.deserializeErgoTree)
      .fold(_ => Values.ErgoTree.fromProposition(FalseLeaf.toSigmaProp).pure, _.pure)

  @inline def extractErgoTreeConstants[F[_]: Applicative: Throws](
    raw: HexString
  ): F[List[(Int, Constant[SType], HexString)]] =
    deserializeErgoTree(raw).map {
      _.constants.zipWithIndex.toList.map { case (c, ix) =>
        val constantStore = new ConstantStore()
        val bw            = SigmaSerializer.startWriter(constantStore)
        constantSerializer.serialize(c, bw)
        val rawValue = HexString.fromStringUnsafe(Base16.encode(bw.toBytes))
        (ix, c, rawValue)
      }
    }

  @inline def deriveErgoTreeTemplateHash[F[_]: Applicative: Throws](ergoTree: HexString): F[ErgoTreeTemplateHash] =
    deserializeErgoTree(ergoTree).map { tree =>
      ErgoTreeTemplateHash.fromStringUnsafe(Base16.encode(Sha256.hash(tree.template)))
    }

  @inline def ergoTreeToAddress[F[_]: Applicative](
    ergoTree: HexString
  )(implicit enc: ErgoAddressEncoder): F[ErgoAddress] =
    Base16
      .decode(ergoTree.unwrapped)
      .flatMap { bytes =>
        enc.fromProposition(treeSerializer.deserializeErgoTree(bytes))
      }
      .fold(_ => (Pay2SAddress(FalseLeaf.toSigmaProp): ErgoAddress).pure, _.pure)

  @inline def addressToErgoTree(
    address: Address
  )(implicit enc: ErgoAddressEncoder): ErgoTree =
    enc
      .fromString(address.unwrapped)
      .map(_.script)
      .get

  @inline def addressToErgoTreeHex(address: Address)(implicit enc: ErgoAddressEncoder): HexString =
    addressToErgoTree(address) |> (tree => HexString.fromStringUnsafe(Base16.encode(tree.bytes)))

  @inline def addressToErgoTreeNewtype(address: Address)(implicit
    enc: ErgoAddressEncoder
  ): org.ergoplatform.explorer.ErgoTree =
    addressToErgoTreeHex(address) |> (tree => org.ergoplatform.explorer.ErgoTree(tree))

  @inline def hexStringToBytes[
    F[_]: CRaise[*[_], Base16DecodingFailed]: Applicative
  ](s: HexString): F[Array[Byte]] =
    Base16
      .decode(s.unwrapped)
      .toEither
      .leftMap(e => Base16DecodingFailed(s, Option(e.getMessage)))
      .toRaise

  @inline def bytesToErgoTree[
    F[_]: CRaise[*[_], ErgoTreeDeserializationFailed]: Applicative
  ](bytes: Array[Byte]): F[ErgoTree] =
    Try {
      treeSerializer.deserializeErgoTree(bytes)
    }.toEither
      .leftMap(e => ErgoTreeDeserializationFailed(bytes, Option(e.getMessage)))
      .toRaise

  /** Extracts ErgoTree's template (serialized tree with placeholders instead of values)
    * @param ergoTree ErgoTree
    * @return serialized ErgoTree's template
    */
  @inline def ergoTreeTemplateBytes[F[_]: CRaise[*[_], ErgoTreeSerializationErr]: Monad](
    ergoTree: ErgoTree
  ): F[Array[Byte]] = {
    val bytes = ergoTree.bytes
    Try {
      val r = SigmaSerializer.startReader(bytes)
      treeSerializer.deserializeHeaderWithTreeBytes(r)._4
    }.toEither
      .leftMap(e => ErgoTreeDeserializationFailed(bytes, Option(e.getMessage)))
      .toRaise
  }

  import cats.instances.list._
  import cats.syntax.traverse._

  @inline def renderEvaluatedValue[T <: SType](ev: EvaluatedValue[T]): Option[(SigmaType, String)] = {
    def goRender[T0 <: SType](ev0: EvaluatedValue[T0]): OptionT[Eval, (SigmaType, String)] =
      ev0.tpe match {
        case SSigmaProp | SGroupElement =>
          ev0 match {
            case SigmaPropConstant(ProveDlogProp(dlog)) =>
              OptionT.some(SigmaType.SimpleKindSigmaType.SSigmaProp -> Base16.encode(dlog.pkBytes))
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
        case SCollectionType(SByte) =>
          OptionT.some(
            SigmaType.SCollection(SigmaType.SimpleKindSigmaType.SByte) ->
            Base16.encode(ev0.value.asInstanceOf[SCollection[SByte.type]#WrappedType].toArray)
          )
        case coll: SCollection[_] =>
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
