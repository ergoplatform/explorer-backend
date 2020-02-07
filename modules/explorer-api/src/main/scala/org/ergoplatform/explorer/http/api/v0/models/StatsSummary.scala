package org.ergoplatform.explorer.http.api.v0.models

final case class StatsSummary(
  blocksCount: Long,
  blocksAvgTime: Long,
  totalCoins: Long,
  totalTransactionsCount: Long,
  totalFee: Long,
  totalOutput: Long,
  estimatedOutput: BigInt,
  totalMinerRevenue: Long,
  percentEarnedTransactionsFees: Double,
  percentTransactionVolume: Double,
  costPerTx: Long,
  lastDifficulty: Long,
  totalHashrate: Long
)
