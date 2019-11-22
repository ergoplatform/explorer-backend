package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.{Address, Id}

/** Entity representing `blocks_info` table.
  * Containing main fields from protocol header and full-block stats.
  */
final case class BlockInfo(
  headerId: Id,
  timestamp: Long,
  height: Long,
  difficulty: Long,
  blockSize: Long,
  blockCoins: Long,
  blockMiningTime: Long,
  txsCount: Long,
  txsSize: Long,
  minerAddress: Address,
  minerReward: Long,
  minerRevenue: Long,
  blockFee: Long,
  blockChainTotalSize: Long,
  totalTxsCount: Long,
  totalCoinsIssued: Long,
  totalMiningTime: Long,
  totalFees: Long,
  totalMinersReward: Long,
  totalCoinsInTxs: Long
)
