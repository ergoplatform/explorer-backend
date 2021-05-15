package org.ergoplatform.explorer.http.api.v1.models

final case class Balance(nanoErgs: Long, tokens: List[TokenAmount])

object Balance {
  def empty: Balance = Balance(0L, List.empty)
}
