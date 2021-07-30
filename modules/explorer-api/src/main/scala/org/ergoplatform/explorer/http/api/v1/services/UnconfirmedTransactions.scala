package org.ergoplatform.explorer.http.api.v1.services

import org.ergoplatform.explorer.Address
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.v1.models.TransactionInfo

trait UnconfirmedTransactions[F[_]] {

  def getByAddress(
    address: Address,
    paging: Paging
  ): F[Items[TransactionInfo]]
}
