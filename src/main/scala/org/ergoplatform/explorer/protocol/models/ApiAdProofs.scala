package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor}

final case class ApiAdProofs(
  headerId: String,
  proofBytes: String,
  digest: String
)

object ApiAdProofs {

  implicit val decoder: Decoder[ApiAdProofs] = { c: HCursor =>
    for {
      headerId   <- c.downField("headerId").as[String]
      proofBytes <- c.downField("proofBytes").as[String]
      digest     <- c.downField("digest").as[String]
    } yield ApiAdProofs(headerId, proofBytes, digest)
  }
}
