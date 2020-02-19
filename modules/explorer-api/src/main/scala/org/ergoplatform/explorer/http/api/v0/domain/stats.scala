package org.ergoplatform.explorer.http.api.v0.domain

import java.util.concurrent.TimeUnit

import cats.Functor
import cats.syntax.functor._
import cats.effect.Clock
import org.ergoplatform.explorer.db.models.BlockInfo
import org.ergoplatform.explorer.http.api.v0.models.StatsSummary

import scala.math.BigDecimal

object stats {

  private val SecondsIn24H: Long = (24 * 60 * 60).toLong
  private val MillisIn24H: Long = SecondsIn24H * 1000L

  private def percentOfFee(fees: Long, minersReward: Long) =
    if (fees + minersReward == 0L) {
      0.0
    } else {
      val result = fees.toDouble / (minersReward.toDouble + fees.toDouble)
      BigDecimal(result * 100).setScale(8, BigDecimal.RoundingMode.HALF_UP).toDouble
    }

  private def percentOfTxVolume(minersReward: Long, totalCoins: Long): Double =
    if (totalCoins == 0L) {
      0.0
    } else {
      val result = minersReward.toDouble / totalCoins.toDouble
      BigDecimal(result * 100).setScale(8, BigDecimal.RoundingMode.HALF_UP).toDouble
    }

  private def hashrateForSecs(difficulty: Long, seconds: Long): Long = (difficulty / seconds) + 1L

  /** Get timestamp to calculate statistics from.
    */
  @inline def getPastTs[F[_]: Clock: Functor]: F[Long] =
    Clock[F].realTime(TimeUnit.MILLISECONDS).map(_ - stats.MillisIn24H)

  @inline def recentToStats(
    blocks: List[BlockInfo],
    totalOutputs: BigDecimal,
    estimatedOutputs: BigDecimal
  ): StatsSummary =
    blocks.sortBy(info => -info.height) match {
      case Nil =>
        ???
      case x :: _ =>
        val blocksCount   = blocks.length.toLong
        val avgMiningTime = blocks.map(_.blockMiningTime).sum / blocksCount
        val coins         = blocks.map(_.blockCoins).sum
        val txsCount      = blocks.map(_.txsCount.toLong).sum
        val totalFee      = blocks.map(_.blockFee).sum
        val minersRevenue = blocks.map(_.minerRevenue).sum
        val minersReward  = blocks.map(_.minerReward).sum
        val hashrate      = hashrateForSecs(blocks.map(_.difficulty).sum, SecondsIn24H)

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
          totalHashrate                 = hashrate
        )
    }
}
