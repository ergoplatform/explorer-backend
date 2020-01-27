package org.ergoplatform.explorer.protocol

import cats.syntax.either._
import cats.syntax.flatMap._
import cats.{Applicative, Monad}
import org.ergoplatform.explorer.Err.RefinementFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.AddressDecodingFailed
import org.ergoplatform.explorer.{Address, HexString}
import org.ergoplatform.{ErgoAddress, ErgoAddressEncoder}
import scorex.util.encode.Base16
import sigmastate.Values.ErgoTree
import sigmastate.serialization.ErgoTreeSerializer
import tofu.Raise.ContravariantRaise
import tofu.syntax.raise._

import scala.util.Try

object utils {

  private val treeSerializer: ErgoTreeSerializer = new ErgoTreeSerializer

  @inline def ergoTreeToAddress(
    ergoTree: String
  )(implicit enc: ErgoAddressEncoder): Try[ErgoAddress] =
    Base16.decode(ergoTree).flatMap { bytes =>
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
    F[_]: ContravariantRaise[*[_], AddressDecodingFailed]
        : ContravariantRaise[*[_], RefinementFailed]
        : Monad
  ](address: Address)(implicit enc: ErgoAddressEncoder): F[HexString] =
    addressToErgoTree[F](address).flatMap(tree => HexString.fromString(Base16.encode(tree.bytes)))
}
