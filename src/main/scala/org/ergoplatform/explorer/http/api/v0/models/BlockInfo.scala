package org.ergoplatform.explorer.http.api.v0.models

final case class BlockInfo(
  id: String,
  height: Int,
  timestamp: Long,
  transactionsCount: Int,
  miner: MinerInfo,
  size: Int,
  difficulty: Long,
  minerReward: Long
)
