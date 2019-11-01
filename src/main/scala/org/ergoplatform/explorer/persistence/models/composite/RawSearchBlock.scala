package org.ergoplatform.explorer.persistence.models.composite

final case class RawSearchBlock(
  id: String,
  height: Long,
  timestamp: Long,
  txsCount: Long,
  minerAddress: String,
  minerName: Option[String],
  blockSize: Long,
  difficulty: Long,
  minerReward: Long
)
