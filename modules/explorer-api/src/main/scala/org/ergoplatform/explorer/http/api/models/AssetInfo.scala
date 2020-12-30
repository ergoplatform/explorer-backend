package org.ergoplatform.explorer.http.api.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.db.models.{Asset, UAsset}
import sttp.tapir.{Schema, Validator}

final case class AssetInfo(tokenId: TokenId, index: Int, amount: Long)

object AssetInfo {

  def apply(asset: UAsset): AssetInfo = AssetInfo(asset.tokenId, asset.index, asset.amount)

  def apply(asset: Asset): AssetInfo = AssetInfo(asset.tokenId, asset.index, asset.amount)

  implicit val codec: Codec[AssetInfo] = deriveCodec

  implicit val schema: Schema[AssetInfo] =
    Schema
      .derive[AssetInfo]
      .modify(_.tokenId)(_.description("Token ID"))
      .modify(_.index)(_.description("Index of the asset in an output"))
      .modify(_.amount)(_.description("Amount of tokens"))

  implicit val validator: Validator[AssetInfo] = Validator.derive
}
