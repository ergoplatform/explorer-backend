package org.ergoplatform.explorer.persistence.models.composite

import org.ergoplatform.explorer.Address

final case class MinerStats(
  minerAddress: Address,
  totalDifficulties: Long,
  totalTime: Long,
  blocksMined: Long,
  minerName: Option[String]
) {
  val verboseName: String = minerName.getOrElse(minerAddress.value.takeRight(8))
}
