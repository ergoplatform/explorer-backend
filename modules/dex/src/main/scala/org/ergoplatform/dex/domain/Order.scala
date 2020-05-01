package org.ergoplatform.dex.domain

import org.ergoplatform.explorer.TokenId

final case class Order[OT <: OrderType](
  `type`: OT,
  pair: (TokenId, TokenId),
  amount: Long,
  limit: Long
)

object Order {

  def mkBuy(pair: (TokenId, TokenId), amount: Long, limit: Long): Order[OrderType.Buy] =
    Order(OrderType.Buy(), pair, amount, limit)

  def mkSell(pair: (TokenId, TokenId), amount: Long, limit: Long): Order[OrderType.Sell] =
    Order(OrderType.Sell(), pair, amount, limit)
}
