package org.ergoplatform.explorer.http.api.v1.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.db.models.aggregates.ExtendedAsset
import org.ergoplatform.explorer.{BoxId, Id, TokenId}
import sttp.tapir.{Schema, Validator}

final case class AssetInfo(
  headerId: Id,
  boxId: BoxId,
  tokenId: TokenId,
  index: Int,
  amount: Long,
  name: Option[String],
  decimals: Option[Int],
  `type`: Option[String]
)

object AssetInfo {

  def apply(asset: ExtendedAsset): AssetInfo =
    AssetInfo(
      asset.headerId,
      asset.boxId,
      asset.tokenId,
      asset.index,
      asset.amount,
      asset.name,
      asset.decimals,
      asset.`type`
    )

  implicit val codec: Codec[AssetInfo] = deriveCodec

  implicit val schema: Schema[AssetInfo] =
    Schema
      .derive[AssetInfo]
      .modify(_.headerId)(_.description("Header ID this asset belongs to"))
      .modify(_.boxId)(_.description("Box ID this asset belongs to"))
      .modify(_.tokenId)(_.description("Token ID"))
      .modify(_.index)(_.description("Index of the asset in an output"))
      .modify(_.amount)(_.description("Amount of tokens"))
      .modify(_.name)(_.description("Name of the asset"))
      .modify(_.decimals)(_.description("Number of decimal places"))
      .modify(_.`type`)(_.description("Type of the asset (token standard)"))

  implicit val validator: Validator[AssetInfo] = Validator.derive
}
