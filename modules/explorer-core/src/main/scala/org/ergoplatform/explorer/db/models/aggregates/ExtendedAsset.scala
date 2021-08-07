package org.ergoplatform.explorer.db.models.aggregates

import org.ergoplatform.explorer.{BlockId, BoxId, TokenId, TokenType}

/** Asset entity enriched with name and decimals num of the asset.
  */
final case class ExtendedAsset(
  tokenId: TokenId,
  boxId: BoxId,
  headerId: BlockId,
  index: Int,
  amount: Long,
  name: Option[String],
  decimals: Option[Int],
  `type`: Option[TokenType]
)
