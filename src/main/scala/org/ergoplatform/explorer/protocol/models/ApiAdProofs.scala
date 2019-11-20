package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor}
import io.circe.refined._
import org.ergoplatform.explorer.{HexString, Id}

final case class ApiAdProofs(
  headerId: Id,
  proofBytes: HexString,
  digest: HexString
)

object ApiAdProofs {

  implicit val decoder: Decoder[ApiAdProofs] = { c: HCursor =>
    for {
      headerId   <- c.downField("headerId").as[Id]
      proofBytes <- c.downField("proofBytes").as[HexString]
      digest     <- c.downField("digest").as[HexString]
    } yield ApiAdProofs(headerId, proofBytes, digest)
  }
}
