package org.ergoplatform.explorer.persistence.models.composite

import org.ergoplatform.explorer.{Address, Id}

final case class RawSearchBlock(
  id: Id,
  height: Int,
  timestamp: Long,
  txsCount: Int,
  minerAddress: Address,
  minerName: Option[String],
  blockSize: Int,
  difficulty: Long,
  minerReward: Long
)
