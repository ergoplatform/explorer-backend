package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, ExtendedOutput}
import sttp.tapir.{Schema, Validator}

final case class DexSellOrderInfo(
  outputInfo: OutputInfo,
  amount: Long
)

object DexSellOrderInfo {

  implicit val codec: Codec[DexSellOrderInfo] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[DexSellOrderInfo] =
    Schema
      .derived[DexSellOrderInfo]
      .modify(_.amount)(_.description("ERG amount"))

  implicit val validator: Validator[DexSellOrderInfo] = schema.validator

  def apply(o: ExtendedOutput, tokenPrice: Long, assets: List[ExtendedAsset]): DexSellOrderInfo =
    new DexSellOrderInfo(OutputInfo(o, assets), tokenPrice)
}
