package org.ergoplatform.explorer.protocol

import cats.syntax.either._
import cats.syntax.flatMap._
import cats.{Applicative, Monad}
import org.ergoplatform.explorer.Err.RefinementFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.AddressDecodingFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.DexErr.ContractParsingErr.{
  Base16DecodingFailed,
  ErgoTreeSerializationErr
}
import org.ergoplatform.explorer.Err.RequestProcessingErr.DexErr.ContractParsingErr.ErgoTreeSerializationErr.{
  ErgoTreeDeserializationFailed,
  ErgoTreeSerializationFailed
}
import org.ergoplatform.explorer.{Address, CRaise, HexString}
import org.ergoplatform.{ErgoAddress, ErgoAddressEncoder}
import scorex.util.encode.Base16
import sigmastate.Values.ErgoTree
import sigmastate.serialization.{ErgoTreeSerializer, SigmaSerializer}
import tofu.syntax.raise._

import scala.util.Try

object utils {

  private val treeSerializer: ErgoTreeSerializer = ErgoTreeSerializer.DefaultSerializer

  // TODO ScalaDoc
  @inline def ergoTreeToAddress(
    ergoTree: HexString
  )(implicit enc: ErgoAddressEncoder): Try[ErgoAddress] =
    Base16.decode(ergoTree.unwrapped).flatMap { bytes =>
      enc.fromProposition(treeSerializer.deserializeErgoTree(bytes))
    }

  // TODO ScalaDoc
  @inline def addressToErgoTree[F[_]: CRaise[*[_], AddressDecodingFailed]: Applicative](
    address: Address
  )(implicit enc: ErgoAddressEncoder): F[ErgoTree] =
    enc
      .fromString(address.unwrapped)
      .map(_.script)
      .toEither
      .leftMap(e => AddressDecodingFailed(address, Option(e.getMessage)))
      .toRaise

  // TODO ScalaDoc
  @inline def addressToErgoTreeHex[
    F[_]: CRaise[*[_], AddressDecodingFailed]: CRaise[*[_], RefinementFailed]: Monad
  ](address: Address)(implicit enc: ErgoAddressEncoder): F[HexString] =
    addressToErgoTree[F](address).flatMap(tree =>
      HexString.fromString(Base16.encode(tree.bytes))
    )

  // TODO ScalaDoc
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

  // TODO ScalaDoc: why just using ergoTree.bytes is not enough?
  @inline def ergoTreeToBytes[
    F[_]: CRaise[*[_], ErgoTreeSerializationFailed]: Applicative
  ](ergoTree: ErgoTree): F[Array[Byte]] =
    Try {
      ergoTree.bytes
    }.toEither
      .leftMap(e => ErgoTreeSerializationFailed(ergoTree, Option(e.getMessage)))
      .toRaise

  /** Extracts ErgoTree's template (serialized tree with placeholders instead of values)
    * @param ergoTree ErgoTree
    * @return serialized ErgoTree's template
    */
  def ergoTreeTemplateBytes[F[_]: CRaise[*[_], ErgoTreeSerializationErr]: Monad](
    ergoTree: ErgoTree
  ): F[Array[Byte]] =
    ergoTreeToBytes[F](ergoTree).flatMap { bytes =>
      Try {
        val r = SigmaSerializer.startReader(bytes)
        treeSerializer.deserializeHeaderWithTreeBytes(r)._4
      }.toEither
        .leftMap(e => ErgoTreeDeserializationFailed(bytes, Option(e.getMessage)))
        .toRaise
    }
}
