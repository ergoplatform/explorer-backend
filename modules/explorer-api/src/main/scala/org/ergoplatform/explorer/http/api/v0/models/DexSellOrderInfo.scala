package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.db.models.Asset
import org.ergoplatform.explorer.db.models.aggregates.DexSellOrderOutput
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class DexSellOrderInfo(
  outputInfo: OutputInfo,
  tokenPrice: Long
)

object DexSellOrderInfo {

  implicit val codec: Codec[DexSellOrderInfo] = deriveCodec

  implicit val schema: Schema[DexSellOrderInfo] =
    implicitly[Derived[Schema[DexSellOrderInfo]]].value

  def apply(o: DexSellOrderOutput, assets: List[Asset]): DexSellOrderInfo =
    new DexSellOrderInfo(OutputInfo(o.extOutput, assets), o.tokenPrice)

}
