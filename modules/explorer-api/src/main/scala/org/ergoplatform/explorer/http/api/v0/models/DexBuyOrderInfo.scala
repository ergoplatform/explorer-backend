package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.db.models.Asset
import org.ergoplatform.explorer.db.models.aggregates.{
  DexBuyOrderOutput,
  DexSellOrderOutput
}
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class DexBuyOrderInfo(
  outputInfo: OutputInfo,
  tokenId: TokenId,
  tokenAmount: Long
)

object DexBuyOrderInfo {

  implicit val codec: Codec[DexBuyOrderInfo] = deriveCodec

  implicit val schema: Schema[DexBuyOrderInfo] =
    implicitly[Derived[Schema[DexBuyOrderInfo]]].value

  def apply(o: DexBuyOrderOutput, assets: List[Asset]): DexBuyOrderInfo =
    new DexBuyOrderInfo(OutputInfo(o.extOutput, assets), o.tokenId, o.tokenAmount)

}
