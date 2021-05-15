package org.ergoplatform.explorer.http.api.v1.models

import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.db.models.aggregates.AggregatedAsset

final case class TokenAmount(tokenId: TokenId, amount: Long, decimals: Int, name: Option[String])

object TokenAmount {

  def apply(a: AggregatedAsset): TokenAmount =
    TokenAmount(a.tokenId, a.totalAmount, a.decimals.getOrElse(0), a.name)
}
