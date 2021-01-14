package org.ergoplatform.explorer.db.models.aggregates

import org.ergoplatform.explorer.{BoxId, TokenId, TokenType}

final case class ExtendedUAsset(
  tokenId: TokenId,
  boxId: BoxId,
  index: Int,
  amount: Long,
  name: Option[String],
  decimals: Option[Int],
  `type`: Option[TokenType]
)
