package org.ergoplatform.explorer.protocol

import cats.Applicative
import cats.syntax.either._
import org.ergoplatform.explorer.algebra.Raise
import org.ergoplatform.explorer.{Address, Err}
import org.ergoplatform.{ErgoAddress, ErgoAddressEncoder}
import org.ergoplatform.explorer.syntax.either._
import scorex.util.encode.Base16
import sigmastate.Values.ErgoTree
import sigmastate.serialization.ErgoTreeSerializer

import scala.util.Try

object utils {

  private val treeSerializer: ErgoTreeSerializer = new ErgoTreeSerializer

  @inline def ergoTreeToAddress(
    ergoTree: String
  )(implicit enc: ErgoAddressEncoder): Try[ErgoAddress] =
    Base16.decode(ergoTree).flatMap { bytes =>
      enc.fromProposition(treeSerializer.deserializeErgoTree(bytes))
    }

  @inline def addressToErgoTree[F[_]: Raise[*[_], Err.AddressDecodingFailed]: Applicative](
    address: Address
  )(implicit enc: ErgoAddressEncoder): F[ErgoTree] =
    enc
      .fromString(address.unwrapped)
      .map(_.script)
      .toEither
      .leftMap(e => Err.AddressDecodingFailed(address, Option(e.getMessage)))
      .liftToRaise
}
