package org.ergoplatform.explorer.db.models.aggregates

import org.ergoplatform.explorer.{BlockId, BoxId, TokenId, TokenType}

final case class AnyAsset(
  tokenId: TokenId,
  boxId: BoxId,
  headerId: Option[BlockId],
  index: Int,
  amount: Long,
  name: Option[String],
  decimals: Option[Int],
  `type`: Option[TokenType]
)
