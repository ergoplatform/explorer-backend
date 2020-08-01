package org.ergoplatform.explorer.protocol.models

import io.circe.Decoder
import org.ergoplatform.explorer.BoxId

/** A model mirroring ErgoTransactionDataInput entity from Ergo node REST API.
  * See `ErgoTransactionDataInput` in `https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml`
  */
final case class ApiDataInput(boxId: BoxId)

object ApiDataInput {
  implicit val decoder: Decoder[ApiDataInput] = _.downField("boxId").as[BoxId].map(ApiDataInput(_))
}
