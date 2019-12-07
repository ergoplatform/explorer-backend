package org.ergoplatform.explorer.protocol.models

import io.circe.Decoder
import org.ergoplatform.explorer.AssetId

/** A model mirroring Asset entity from Ergo node REST API.
  * See `Asset` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
final case class ApiAsset(
  tokenId: AssetId,
  amount: Long
)

object ApiAsset {

  implicit val decoder: Decoder[ApiAsset] = { cursor =>
    for {
      id  <- cursor.downField("tokenId").as[AssetId]
      amt <- cursor.downField("amount").as[Long]
    } yield ApiAsset(id, amt)
  }
}
