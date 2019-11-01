package org.ergoplatform.explorer.persistence.models

import org.ergoplatform.explorer.{Address, Id}

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
