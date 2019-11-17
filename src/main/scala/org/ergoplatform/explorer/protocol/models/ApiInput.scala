package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor}

final case class ApiInput(boxId: String, spendingProof: ApiSpendingProof)

object ApiInput {

  implicit val decoder: Decoder[ApiInput] = { c: HCursor =>
    for {
      boxId         <- c.downField("boxId").as[String]
      spendingProof <- c.downField("spendingProof").as[ApiSpendingProof]
    } yield ApiInput(boxId, spendingProof)
  }
}
