package org.ergoplatform.explorer.protocol.models

import io.circe.refined._
import io.circe.{Decoder, HCursor, Json}
import org.ergoplatform.explorer.{BoxId, HexString}

/** A model mirroring ErgoTransactionOutput entity from Ergo node REST API.
  * See `ErgoTransactionOutput` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
final case class ApiOutput(
  boxId: BoxId,
  value: Long,
  creationHeight: Int,
  ergoTree: HexString,
  assets: List[ApiAsset],
  additionalRegisters: Json
)

object ApiOutput {

  implicit val decoder: Decoder[ApiOutput] = { c: HCursor =>
    for {
      boxId               <- c.downField("boxId").as[BoxId]
      value               <- c.downField("value").as[Long]
      creationHeight      <- c.downField("creationHeight").as[Int]
      ergoTree            <- c.downField("ergoTree").as[HexString]
      assets              <- c.downField("assets").as[List[ApiAsset]]
      additionalRegisters <- c.downField("additionalRegisters").as[Json]
    } yield ApiOutput(boxId, value, creationHeight, ergoTree, assets, additionalRegisters)
  }
}
