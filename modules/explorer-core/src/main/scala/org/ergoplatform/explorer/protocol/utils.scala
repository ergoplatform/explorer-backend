package org.ergoplatform.explorer.protocol

import cats.syntax.either._
import cats.{Applicative, Monad}
import org.ergoplatform.explorer.Err.RefinementFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.AddressDecodingFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.DexErr.ContractParsingErr.ErgoTreeSerializationErr.ErgoTreeDeserializationFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.DexErr.ContractParsingErr.{
  Base16DecodingFailed,
  ErgoTreeSerializationErr
}
import org.ergoplatform.explorer.{Address, CRaise, HexString}
import org.ergoplatform.{ErgoAddress, ErgoAddressEncoder}
import scorex.util.encode.Base16
import sigmastate.Values
import sigmastate.Values.ErgoTree
import sigmastate.serialization.{ErgoTreeSerializer, SigmaSerializer}
import tofu.Throws
import tofu.syntax.monadic._
import tofu.syntax.raise._

import scala.util.Try

object utils {

  private val treeSerializer: ErgoTreeSerializer = ErgoTreeSerializer.DefaultSerializer

  @inline def deserializeErgoTree[F[_]: Applicative: Throws](raw: HexString): F[Values.ErgoTree] =
    Base16.decode(raw.unwrapped).map(treeSerializer.deserializeErgoTree).fold(_.raise, _.pure)

  @inline def deriveErgoTreeTemplate[F[_]: Applicative: Throws](ergoTree: HexString): F[HexString] =
    deserializeErgoTree(ergoTree).map(tree => HexString.fromStringUnsafe(Base16.encode(tree.template)))

  @inline def ergoTreeToAddress[F[_]: Applicative: Throws](
    ergoTree: HexString
  )(implicit enc: ErgoAddressEncoder): F[ErgoAddress] =
    Base16
      .decode(ergoTree.unwrapped)
      .flatMap { bytes =>
        enc.fromProposition(treeSerializer.deserializeErgoTree(bytes))
      }
      .fold(_.raise, _.pure)

  @inline def addressToErgoTree[F[_]: CRaise[*[_], AddressDecodingFailed]: Applicative](
    address: Address
  )(implicit enc: ErgoAddressEncoder): F[ErgoTree] =
    enc
      .fromString(address.unwrapped)
      .map(_.script)
      .toEither
      .leftMap(e => AddressDecodingFailed(address, Option(e.getMessage)))
      .toRaise

  @inline def addressToErgoTreeHex[
    F[_]: CRaise[*[_], AddressDecodingFailed]: CRaise[*[_], RefinementFailed]: Monad
  ](address: Address)(implicit enc: ErgoAddressEncoder): F[HexString] =
    addressToErgoTree[F](address).flatMap(tree => HexString.fromString(Base16.encode(tree.bytes)))

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
  def ergoTreeTemplateBytes[F[_]: CRaise[*[_], ErgoTreeSerializationErr]: Monad](
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
}
