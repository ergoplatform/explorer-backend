package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor, Json}

final case class ApiBlockExtension(
  headerId: String,
  digest: String,
  fields: Json
)

object ApiBlockExtension {

  implicit val decoder: Decoder[ApiBlockExtension] = { c: HCursor =>
    for {
      headerId <- c.downField("headerId").as[String]
      digest   <- c.downField("digest").as[String]
      fields   <- c.downField("fields").as[Json]
    } yield ApiBlockExtension(headerId, digest, fields)
  }
}
