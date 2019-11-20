package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor, Json}
import io.circe.refined._
import org.ergoplatform.explorer.HexString

final case class ApiSpendingProof(proofBytes: HexString, extension: Json)

object ApiSpendingProof {

  implicit val decoder: Decoder[ApiSpendingProof] = { c: HCursor =>
    for {
      proofBytes <- c.downField("proofBytes").as[HexString]
      extension  <- c.downField("extension").as[Json]
    } yield ApiSpendingProof(proofBytes, extension)
  }
}
