package org.ergoplatform.explorer.http.api.v0.models

import scala.math.BigDecimal

final case class StatsSummary(
  blocksCount: Long,
  blocksAvgTime: Long,
  totalCoins: Long,
  totalTransactionsCount: Long,
  totalFee: Long,
  totalOutput: BigDecimal,
  estimatedOutput: BigDecimal,
  totalMinerRevenue: Long,
  percentEarnedTransactionsFees: Double,
  percentTransactionVolume: Double,
  costPerTx: Long,
  lastDifficulty: Long,
  totalHashrate: Long
)
