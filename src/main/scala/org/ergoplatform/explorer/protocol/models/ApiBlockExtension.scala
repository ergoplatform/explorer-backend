package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor, Json}
import io.circe.refined._
import org.ergoplatform.explorer.{HexString, Id}

final case class ApiBlockExtension(
  headerId: Id,
  digest: HexString,
  fields: Json
)

object ApiBlockExtension {

  implicit val decoder: Decoder[ApiBlockExtension] = { c: HCursor =>
    for {
      headerId <- c.downField("headerId").as[Id]
      digest   <- c.downField("digest").as[HexString]
      fields   <- c.downField("fields").as[Json]
    } yield ApiBlockExtension(headerId, digest, fields)
  }
}
