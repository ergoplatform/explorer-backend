package org.ergoplatform.explorer.http.api.v0.models

final case class BlockChainInfo(
  version: String,
  supply: Long,
  transactionAverage: Int, // avg. number of transactions per block.
  hashRate: Long
)

object BlockChainInfo {

  def empty: BlockChainInfo = BlockChainInfo("0.0.0", 0L, 0, 0L)
}
