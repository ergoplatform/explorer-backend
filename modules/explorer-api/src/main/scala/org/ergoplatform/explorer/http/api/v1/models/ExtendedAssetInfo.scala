package org.ergoplatform.explorer.http.api.v1.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.db.models.Asset
import org.ergoplatform.explorer.{BoxId, Id, TokenId}
import sttp.tapir.{Schema, Validator}

final case class ExtendedAssetInfo(headerId: Id, boxId: BoxId, tokenId: TokenId, index: Int, amount: Long)

object ExtendedAssetInfo {

  def apply(asset: Asset): ExtendedAssetInfo =
    ExtendedAssetInfo(asset.headerId, asset.boxId, asset.tokenId, asset.index, asset.amount)

  implicit val codec: Codec[ExtendedAssetInfo] = deriveCodec

  implicit val schema: Schema[ExtendedAssetInfo] =
    Schema
      .derive[ExtendedAssetInfo]
      .modify(_.headerId)(_.description("Header ID this asset belongs to"))
      .modify(_.boxId)(_.description("Box ID this asset belongs to"))
      .modify(_.tokenId)(_.description("Token ID"))
      .modify(_.index)(_.description("Index of the asset in an output"))
      .modify(_.amount)(_.description("Amount of tokens"))

  implicit val validator: Validator[ExtendedAssetInfo] = Validator.derive
}
