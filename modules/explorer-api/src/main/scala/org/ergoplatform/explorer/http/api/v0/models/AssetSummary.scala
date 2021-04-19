package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.TokenId
import sttp.tapir.{Schema, Validator}

final case class AssetSummary(tokenId: TokenId, amount: Long, name: Option[String], decimals: Option[Int])

object AssetSummary {

  implicit val codec: Codec[AssetSummary] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[AssetSummary] =
    Schema
      .derived[AssetSummary]
      .modify(_.tokenId)(_.description("Token ID"))
      .modify(_.amount)(_.description("Amount of tokens"))
      .modify(_.decimals)(_.description("Number of decimal places"))
      .modify(_.name)(_.description("Name of a token"))

  implicit val validator: Validator[AssetSummary] = schema.validator
}
