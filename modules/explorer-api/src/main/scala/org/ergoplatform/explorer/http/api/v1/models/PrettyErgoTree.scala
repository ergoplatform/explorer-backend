package org.ergoplatform.explorer.http.api.v1.models

import org.ergoplatform.explorer.{HexString, PrettyPrintErgoTree}
import sigma.VersionContext
import sigma.serialization.ErgoTreeSerializer.DefaultSerializer
import sigma.serialization.SerializerException

object PrettyErgoTree {
  def fromString(s: String) : Either[PrettyErgoTreeError, ErgoTreeHuman] = {
    HexString.fromString[Either[Throwable, *]](s) match {
      case Left(_) => Left(PrettyErgoTreeError.BadEncoding)
      case Right(hexString) => fromHexString(hexString)
    }
  }

  def fromHexString(h: HexString): Either[PrettyErgoTreeError, ErgoTreeHuman] = {
    try {
      val ergoTree = {
        val version = VersionContext.V6SoftForkVersion
        VersionContext.withVersions(version, version) {
          DefaultSerializer.deserializeErgoTree(h.bytes)
        }
      }
      ergoTree.root match {
        case Left(_) => Left(PrettyErgoTreeError.UnparsedErgoTree)
        case Right(value) => 
          val script = PrettyPrintErgoTree.prettyPrint(value, width = 160)
          val constants = ergoTree.constants.zipWithIndex.map { case (c, i) => s"$i: ${c.value}" }.mkString("\n")
          Right(ErgoTreeHuman(constants, script))
      }
    } catch {
      case se: SerializerException => Left(PrettyErgoTreeError.DeserializeException(se.message))
    }
  }
}

sealed trait PrettyErgoTreeError
object PrettyErgoTreeError {
  case object BadEncoding extends PrettyErgoTreeError
  case class DeserializeException(msg: String) extends PrettyErgoTreeError
  case object UnparsedErgoTree extends PrettyErgoTreeError
}