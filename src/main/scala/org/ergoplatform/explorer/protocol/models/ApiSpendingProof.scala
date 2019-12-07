package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor, Json}
import io.circe.refined._
import org.ergoplatform.explorer.HexString

/** A model mirroring SpendingProof entity from Ergo node REST API.
  * See `SpendingProof` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
final case class ApiSpendingProof(proofBytes: HexString, extension: Json)

object ApiSpendingProof {

  implicit val decoder: Decoder[ApiSpendingProof] = { c: HCursor =>
    for {
      proofBytes <- c.downField("proofBytes").as[HexString]
      extension  <- c.downField("extension").as[Json]
    } yield ApiSpendingProof(proofBytes, extension)
  }
}
