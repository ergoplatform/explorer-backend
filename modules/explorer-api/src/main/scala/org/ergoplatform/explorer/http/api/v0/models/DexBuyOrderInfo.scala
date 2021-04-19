package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, ExtendedOutput}
import sttp.tapir.{Schema, Validator}

final case class DexBuyOrderInfo(
  outputInfo: OutputInfo,
  tokenId: TokenId,
  tokenAmount: Long
)

object DexBuyOrderInfo {

  implicit val codec: Codec[DexBuyOrderInfo] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[DexBuyOrderInfo] = Schema.derived

  implicit val validator: Validator[DexBuyOrderInfo] = schema.validator

  def apply(
    output: ExtendedOutput,
    tokenId: TokenId,
    tokenAmount: Long,
    assets: List[ExtendedAsset]
  ): DexBuyOrderInfo =
    new DexBuyOrderInfo(OutputInfo(output, assets), tokenId, tokenAmount)
}
