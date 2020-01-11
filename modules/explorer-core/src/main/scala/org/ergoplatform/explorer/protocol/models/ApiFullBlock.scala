package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor}

/** A model mirroring FullBlock entity from Ergo node REST API.
  * See `FullBlock` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
final case class ApiFullBlock(
  header: ApiHeader,
  transactions: ApiBlockTransactions,
  extension: ApiBlockExtension,
  adProofs: Option[ApiAdProof],
  size: Int
)

object ApiFullBlock {

  implicit val decoder: Decoder[ApiFullBlock] = { c: HCursor =>
    for {
      header       <- c.downField("header").as[ApiHeader]
      transactions <- c.downField("blockTransactions").as[ApiBlockTransactions]
      extension    <- c.downField("extension").as[ApiBlockExtension]
      adProofs <- c.downField("adProofs").as[ApiAdProof] match {
                   case Left(_)       => Right(None)
                   case Right(proofs) => Right(Some(proofs))
                 }
      size <- c.downField("size").as[Int]
    } yield ApiFullBlock(header, transactions, extension, adProofs, size)
  }
}
