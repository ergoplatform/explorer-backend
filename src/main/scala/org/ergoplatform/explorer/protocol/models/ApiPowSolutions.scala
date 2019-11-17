package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor}

final case class ApiPowSolutions(pk: String, w: String, n: String, d: String)

object ApiPowSolutions {

  implicit val jsonDecoder: Decoder[ApiPowSolutions] = { c: HCursor =>
    for {
      pk <- c.downField("pk").as[String]
      w  <- c.downField("w").as[String]
      n  <- c.downField("n").as[String]
      d  <- c.downField("d").as[BigInt]
    } yield ApiPowSolutions(pk, w, n, d.toString())
  }
}
