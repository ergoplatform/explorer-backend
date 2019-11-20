package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor}
import io.circe.refined._
import org.ergoplatform.explorer.HexString

final case class ApiPowSolutions(pk: HexString, w: HexString, n: HexString, d: String)

object ApiPowSolutions {

  implicit val jsonDecoder: Decoder[ApiPowSolutions] = { c: HCursor =>
    for {
      pk <- c.downField("pk").as[HexString]
      w  <- c.downField("w").as[HexString]
      n  <- c.downField("n").as[HexString]
      d  <- c.downField("d").as[BigInt]
    } yield ApiPowSolutions(pk, w, n, d.toString())
  }
}
