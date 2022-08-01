package org.ergoplatform.explorer.http.api.models

import org.ergoplatform.explorer.HexString
import sigmastate.PrettyPrintErgoTree
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer

object PrettyErgoTree {
  def humanErgoTree(i: HexString) : (String, String) = {
    val ergoTree = DefaultSerializer.deserializeErgoTree(i.bytes)
    val humanErgoTree = ergoTree.root match {
      case Left(_) => "could not parse ergoTree"
      case Right(value) => PrettyPrintErgoTree.prettyPrint(value, width = 160)
    }
    val constants = ergoTree.constants.zipWithIndex.map { case (c, i) => s"$i: ${c.value}" }.mkString("\n")
    (constants, humanErgoTree)
  }
}
