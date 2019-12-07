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
  blockSize: Long,           // block size (bytes)
  blockCoins: Long,
  blockMiningTime: Long,
  txsCount: Long,
  txsSize: Long,             // total size of all transactions in this block (bytes)
  minerAddress: Address,
  minerReward: Long,
  minerRevenue: Long,
  blockFee: Long,
  blockChainTotalSize: Long, // cumulative blockchain size including this block
  totalTxsCount: Long,
  totalCoinsIssued: Long,    // number of nERGs issued in block
  totalMiningTime: Long,
  totalFees: Long,           // total number of nERGs in block miner received as a fee
  totalMinersReward: Long,
  totalCoinsInTxs: Long      // total number of nERGs in all blocks
)
