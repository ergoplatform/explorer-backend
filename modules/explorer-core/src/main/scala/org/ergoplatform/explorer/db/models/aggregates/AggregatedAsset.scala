package org.ergoplatform.explorer.db.models.aggregates

import org.ergoplatform.explorer.TokenId

final case class AggregatedAsset(
  tokenId: TokenId,
  totalAmount: Long,
  name: Option[String],
  decimals: Option[Int]
)
