package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

final case class ApiAsset(
  tokenId: String,
  amount: Long
)

object ApiAsset {

  implicit val encode: Encoder[ApiAsset] = { obj =>
    Json.obj(
      "tokenId" -> obj.tokenId.asJson,
      "amount"  -> obj.amount.asJson
    )
  }

  implicit val decoder: Decoder[ApiAsset] = { cursor =>
    for {
      id  <- cursor.downField("tokenId").as[String]
      amt <- cursor.downField("amount").as[Long]
    } yield ApiAsset(id, amt)
  }

}
