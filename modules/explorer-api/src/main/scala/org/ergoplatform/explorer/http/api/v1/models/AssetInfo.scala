package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.explorer.db.models.aggregates.ExtendedAsset
import org.ergoplatform.explorer.{BlockId, BoxId, TokenId, TokenType}
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class AssetInfo(
  headerId: BlockId,
  boxId: BoxId,
  tokenId: TokenId,
  index: Int,
  amount: Long,
  name: Option[String],
  decimals: Option[Int],
  `type`: Option[TokenType]
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

  implicit val schema: Schema[AssetInfo] =
    Schema
      .derived[AssetInfo]
      .modify(_.headerId)(_.description("Header ID this asset belongs to"))
      .modify(_.boxId)(_.description("Box ID this asset belongs to"))
      .modify(_.tokenId)(_.description("Token ID"))
      .modify(_.index)(_.description("Index of the asset in an output"))
      .modify(_.amount)(_.description("Amount of tokens"))
      .modify(_.name)(_.description("Name of the asset"))
      .modify(_.decimals)(_.description("Number of decimal places"))
      .modify(_.`type`)(_.description("Type of the asset (token standard)"))

  implicit val validator: Validator[AssetInfo] = schema.validator
}
