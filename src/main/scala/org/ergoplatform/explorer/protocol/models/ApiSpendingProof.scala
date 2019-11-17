package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor, Json}

final case class ApiSpendingProof(proofBytes: String, extension: Json)

object ApiSpendingProof {

  implicit val decoder: Decoder[ApiSpendingProof] = { c: HCursor =>
    for {
      proofBytes <- c.downField("proofBytes").as[String]
      extension  <- c.downField("extension").as[Json]
    } yield ApiSpendingProof(proofBytes, extension)
  }
}
