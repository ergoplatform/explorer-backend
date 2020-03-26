package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.db.models.Asset
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class DexSellOrderInfo(
  outputInfo: OutputInfo,
  amount: Long
)

object DexSellOrderInfo {

  implicit val codec: Codec[DexSellOrderInfo] = deriveCodec

  implicit val schema: Schema[DexSellOrderInfo] =
    implicitly[Derived[Schema[DexSellOrderInfo]]].value
      .modify(_.amount)(_.description("ERG amount"))

  def apply(o: ExtendedOutput, tokenPrice: Long, assets: List[Asset]): DexSellOrderInfo =
    new DexSellOrderInfo(OutputInfo(o, assets), tokenPrice)
}
