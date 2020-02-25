package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

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

object StatsSummary {

  def empty: StatsSummary =
    StatsSummary(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0d, 0d, 0L, 0L, 0L)

  implicit val codec: Codec[StatsSummary] = deriveCodec

  implicit val schema: Schema[StatsSummary] =
    implicitly[Derived[Schema[StatsSummary]]].value
}
