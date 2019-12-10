package org.ergoplatform.explorer.protocol

import org.ergoplatform.{ErgoAddress, ErgoAddressEncoder}
import scorex.util.encode.Base16
import sigmastate.Values.ErgoTree
import sigmastate.serialization.ErgoTreeSerializer

import scala.util.Try

// TODO move this code to sigmastate ErgoAddress
object utils {

  private val treeSerializer: ErgoTreeSerializer = new ErgoTreeSerializer

  @inline def ergoTreeToAddress(
    ergoTree: String
  )(implicit enc: ErgoAddressEncoder): Try[ErgoAddress] =
    Base16.decode(ergoTree).flatMap { bytes =>
      enc.fromProposition(treeSerializer.deserializeErgoTree(bytes))
    }

  @inline def addressToErgoTree(
    address: String
  )(implicit enc: ErgoAddressEncoder): Try[ErgoTree] =
    enc.fromString(address).map(_.script)
}
