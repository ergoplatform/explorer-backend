package org.ergoplatform.explorer.db.models.aggregates

import org.ergoplatform.explorer.Address

final case class MinerStats(
  minerAddress: Address,
  totalDifficulties: Long,
  totalTime: Long,
  blocksMined: Int,
  minerName: Option[String]
) {
  def verboseName: String =
    minerName.getOrElse(minerAddress.unwrapped.takeRight(8))
}
