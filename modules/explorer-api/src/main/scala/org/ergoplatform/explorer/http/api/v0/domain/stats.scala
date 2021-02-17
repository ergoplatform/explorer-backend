package org.ergoplatform.explorer.http.api.v0.domain

import java.util.concurrent.TimeUnit

import cats.Functor
import cats.syntax.functor._
import cats.effect.Clock
import org.ergoplatform.explorer.db.models.BlockStats
import org.ergoplatform.explorer.http.api.v0.models.StatsSummary

import scala.math.BigDecimal

object stats {

  private val SecondsIn24H: Long = (24 * 60 * 60).toLong
  private val MillisIn24H: Long  = SecondsIn24H * 1000L

  // Get percent of fees in total miner rewards.
  private def percentOfFee(fees: Long, minersReward: Long): Double =
    if (fees + minersReward == 0L) {
      0.0
    } else {
      // TODO check overflow of operations
      val result = fees.toDouble / (minersReward.toDouble + fees.toDouble)
      BigDecimal(result * 100).setScale(8, BigDecimal.RoundingMode.HALF_UP).toDouble
    }

  // Get percent of miner rewards in total coins number.
  private def percentOfTxVolume(minersReward: Long, totalCoins: Long): Double =
    if (totalCoins == 0L) {
      0.0
    } else {
      val result = minersReward.toDouble / totalCoins.toDouble
      BigDecimal(result * 100).setScale(8, BigDecimal.RoundingMode.HALF_UP).toDouble
    }

  @inline def dailyHashRate(difficulty: Long): Long = (difficulty / SecondsIn24H) + 1L

  /** Get timestamp millis 24h back.
    */
  @inline def getPastPeriodTsMillis[F[_]: Clock: Functor]: F[Long] =
    Clock[F].realTime(TimeUnit.MILLISECONDS).map(_ - stats.MillisIn24H)

  /** Obtain stats from recent blocks.
    */
  // todo: Use DB aggregation
  @inline def recentToStats(
    blocks: List[BlockStats],
    totalOutputs: BigDecimal,
    estimatedOutputs: BigDecimal
  ): StatsSummary =
    blocks.sortBy(info => -info.height) match {
      case Nil =>
        StatsSummary.empty
      case x :: _ =>
        val blocksCount   = blocks.size
        val avgMiningTime = blocks.flatMap(_.blockMiningTime).sum / blocksCount
        val coins         = blocks.map(_.blockCoins).sum
        val txsCount      = blocks.map(_.txsCount.toLong).sum
        val totalFee      = blocks.map(_.blockFee).sum
        val minersRevenue = blocks.map(_.minerRevenue).sum
        val minersReward  = blocks.map(_.minerReward).sum
        val hashRate      = dailyHashRate(blocks.map(_.difficulty).sum)

        StatsSummary(
          blocksCount                   = blocksCount,
          blocksAvgTime                 = avgMiningTime,
          totalCoins                    = minersReward,
          totalTransactionsCount        = txsCount,
          totalFee                      = totalFee,
          totalOutput                   = totalOutputs,
          estimatedOutput               = estimatedOutputs,
          totalMinerRevenue             = minersRevenue,
          percentEarnedTransactionsFees = percentOfFee(totalFee, minersReward),
          percentTransactionVolume      = percentOfTxVolume(minersReward, coins),
          costPerTx                     = if (txsCount == 0L) 0L else minersRevenue / txsCount,
          lastDifficulty                = x.difficulty,
          totalHashrate                 = hashRate
        )
    }
}
