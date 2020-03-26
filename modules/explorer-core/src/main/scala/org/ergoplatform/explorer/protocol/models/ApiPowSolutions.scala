package org.ergoplatform.explorer.protocol.models

import io.circe.refined._
import io.circe.{Decoder, HCursor}
import org.ergoplatform.explorer.HexString

/** A model mirroring PowSolutions entity from Ergo node REST API.
  * See `PowSolutions` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
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
