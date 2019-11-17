package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor}

final case class ApiFullBlock(
  header: ApiHeader,
  transactions: ApiBlockTransactions,
  extension: ApiBlockExtension,
  adProofs: Option[ApiAdProofs],
  size: Long
)

object ApiFullBlock {

  implicit val decoder: Decoder[ApiFullBlock] = { c: HCursor =>
    for {
      header       <- c.downField("header").as[ApiHeader]
      transactions <- c.downField("blockTransactions").as[ApiBlockTransactions]
      extension    <- c.downField("extension").as[ApiBlockExtension]
      adProofs <- c.downField("adProofs").as[ApiAdProofs] match {
        case Left(_)       => Right(None)
        case Right(proofs) => Right(Some(proofs))
      }
      size <- c.downField("size").as[Long]
    } yield ApiFullBlock(header, transactions, extension, adProofs, size)
  }
}
