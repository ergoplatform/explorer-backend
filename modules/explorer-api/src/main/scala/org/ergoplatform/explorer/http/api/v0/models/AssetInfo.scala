package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.db.models.UAsset
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class AssetInfo(tokenId: TokenId, index: Int, amount: Long)

object AssetInfo {

  def apply(asset: UAsset): AssetInfo = AssetInfo(asset.tokenId, asset.index, asset.amount)

  implicit val codec: Codec[AssetInfo] = deriveCodec

  implicit val schema: Schema[AssetInfo] =
    implicitly[Derived[Schema[AssetInfo]]].value
      .modify(_.tokenId)(_.description("Token ID"))
      .modify(_.index)(_.description("Index of the asset in an output"))
      .modify(_.amount)(_.description("Amount of tokens"))
}
