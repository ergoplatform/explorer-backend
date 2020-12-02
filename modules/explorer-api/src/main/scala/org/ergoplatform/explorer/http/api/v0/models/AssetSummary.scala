package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.TokenId
import sttp.tapir.{Schema, Validator}

final case class AssetSummary(tokenId: TokenId, amount: Long)

object AssetSummary {

  implicit val codec: Codec[AssetSummary] = deriveCodec

  implicit val schema: Schema[AssetSummary] =
    Schema
      .derive[AssetSummary]
      .modify(_.tokenId)(_.description("Token ID"))
      .modify(_.amount)(_.description("Amount of tokens"))

  implicit val validator: Validator[AssetSummary] = Validator.derive
}
