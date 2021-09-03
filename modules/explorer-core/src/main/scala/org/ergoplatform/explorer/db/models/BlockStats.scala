package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.{Address, BlockId}

/** Represents `blocks_info` table.
  * Containing main fields from protocol header and full-block stats.
  */
final case class BlockStats(
  headerId: BlockId,
  timestamp: Long,
  height: Int,
  difficulty: Long,
  blockSize: Int, // block size (bytes)
  blockCoins: Long, // total amount of nERGs in the block
  blockMiningTime: Option[Long], // block mining time
  txsCount: Int, // number of txs in the block
  txsSize: Int, // total size of all transactions in this block (bytes)
  minerAddress: Address,
  minerReward: Long, // total amount of nERGs miner received from coinbase
  minerRevenue: Long, // total amount of nERGs miner received as a reward (coinbase + fee)
  blockFee: Long, // total amount of transaction fee in the block (nERG)
  blockChainTotalSize: Long, // cumulative blockchain size including this block
  totalTxsCount: Long, // total number of txs in all blocks in the chain
  totalCoinsIssued: Long, // amount of nERGs issued in the block
  totalMiningTime: Long, // mining time of all the blocks in the chain
  totalFees: Long, // total amount of nERGs all miners received as a fee
  totalMinersReward: Long, // total amount of nERGs all miners received as a reward for all time
  totalCoinsInTxs: Long, // total amount of nERGs in all blocks
  maxTxGix: Long, // Global index of the last transaction in the block
  maxBoxGix: Long, // Global index of the last output in the last transaction in the block
  mainChain: Boolean
)
