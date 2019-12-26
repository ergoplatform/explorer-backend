package org.ergoplatform.explorer.http.api.v0.models

final case class BlockChainInfo(
  version: String,
  supply: Long,
  averageTransactionPerBlock: Int,
  hashRate: Long
)
