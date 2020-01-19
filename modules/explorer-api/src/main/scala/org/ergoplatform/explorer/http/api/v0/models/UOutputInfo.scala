package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Json
import org.ergoplatform.explorer.{BoxId, HexString}
import org.ergoplatform.explorer.db.models.{UAsset, UOutput}

final case class UOutputInfo(
  boxId: BoxId,
  value: Long,
  creationHeight: Int,
  ergoTree: HexString,
  assets: List[AssetInfo],
  additionalRegisters: Json
)

object UOutputInfo {

  def apply(out: UOutput, assets: List[UAsset]): UOutputInfo =
    UOutputInfo(
      out.boxId,
      out.value,
      out.creationHeight,
      out.ergoTree,
      assets.map(AssetInfo.apply),
      out.additionalRegisters
    )
}
