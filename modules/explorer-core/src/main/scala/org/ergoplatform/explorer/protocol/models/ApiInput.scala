package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor}
import org.ergoplatform.explorer.BoxId

/** A model mirroring ErgoTransactionInput entity from Ergo node REST API.
  * See `ErgoTransactionInput` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
final case class ApiInput(boxId: BoxId, spendingProof: ApiSpendingProof)

object ApiInput {

  implicit val decoder: Decoder[ApiInput] = { c: HCursor =>
    for {
      boxId         <- c.downField("boxId").as[BoxId]
      spendingProof <- c.downField("spendingProof").as[ApiSpendingProof]
    } yield ApiInput(boxId, spendingProof)
  }
}
