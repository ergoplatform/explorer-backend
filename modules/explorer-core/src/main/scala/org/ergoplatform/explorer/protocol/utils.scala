package org.ergoplatform.explorer.protocol

import cats.syntax.either._
import cats.syntax.flatMap._
import cats.{Applicative, Monad}
import org.ergoplatform.explorer.Err.RefinementFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.ContractParsingErr.{
  Base16DecodingFailed,
  ErgoTreeDeserializationFailed
}
import org.ergoplatform.explorer.Err.RequestProcessingErr.AddressDecodingFailed
import org.ergoplatform.explorer.{Address, HexString}
import org.ergoplatform.{ErgoAddress, ErgoAddressEncoder}
import scorex.util.encode.Base16
import sigmastate.Values.ErgoTree
import sigmastate.serialization.{ErgoTreeSerializer, SigmaSerializer}
import tofu.Raise.ContravariantRaise
import tofu.syntax.raise._

import scala.util.Try

object utils {

  private val treeSerializer: ErgoTreeSerializer = ErgoTreeSerializer.DefaultSerializer

  @inline def ergoTreeToAddress(
    ergoTree: HexString
  )(implicit enc: ErgoAddressEncoder): Try[ErgoAddress] =
    Base16.decode(ergoTree.unwrapped).flatMap { bytes =>
      enc.fromProposition(treeSerializer.deserializeErgoTree(bytes))
    }

  @inline def addressToErgoTree[F[_]: ContravariantRaise[*[_], AddressDecodingFailed]: Applicative](
    address: Address
  )(implicit enc: ErgoAddressEncoder): F[ErgoTree] =
    enc
      .fromString(address.unwrapped)
      .map(_.script)
      .toEither
      .leftMap(e => AddressDecodingFailed(address, Option(e.getMessage)))
      .toRaise

  @inline def addressToErgoTreeHex[
    F[_]: ContravariantRaise[*[_], AddressDecodingFailed]: ContravariantRaise[*[_], RefinementFailed]: Monad
  ](address: Address)(implicit enc: ErgoAddressEncoder): F[HexString] =
    addressToErgoTree[F](address).flatMap(tree =>
      HexString.fromString(Base16.encode(tree.bytes))
    )

  @inline def hexStringBase16ToBytes[
    F[_]: ContravariantRaise[*[_], Base16DecodingFailed]: Applicative
  ](s: HexString): F[Array[Byte]] =
    Base16
      .decode(s.unwrapped)
      .toEither
      .leftMap(e => Base16DecodingFailed(s, Option(e.getMessage)))
      .toRaise

  @inline def bytesToErgoTree[
    F[_]: ContravariantRaise[*[_], ErgoTreeDeserializationFailed]: Applicative
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
  def ergoTreeTemplateBytes(ergoTree: ErgoTree): Array[Byte] = {
    val r = SigmaSerializer.startReader(ergoTree.bytes)
    treeSerializer.deserializeHeaderWithTreeBytes(r)._4
  }
}
