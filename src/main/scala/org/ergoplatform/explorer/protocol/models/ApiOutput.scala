package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor, Json}
import io.circe.refined._
import org.ergoplatform.explorer.{BoxId, HexString}

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
