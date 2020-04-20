package org.ergoplatform.explorer.http.api.v0.services

import java.util.concurrent.TimeUnit

import cats.effect.{Clock, Sync}
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{FlatMap, Functor, Monad}
import fs2.Stream
import mouse.anyf._
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories.{
  BlockInfoRepo,
  HeaderRepo,
  OutputRepo,
  TransactionRepo
}
import org.ergoplatform.explorer.http.api.v0.domain.stats
import org.ergoplatform.explorer.http.api.v0.models.{
  BlockChainInfo,
  ChartPoint,
  HashRateDistributionSegment,
  StatsSummary
}
import org.ergoplatform.explorer.settings.ProtocolSettings

import scala.concurrent.duration.FiniteDuration

trait StatsService[F[_]] {

  /** Get blockchain statistics summary.
    */
  def getCurrentStats: F[StatsSummary]

  /** Get short blockchain info.
    */
  def getBlockChainInfo: F[BlockChainInfo]

  def getTotalCoins(timespan: FiniteDuration): F[List[ChartPoint]]

  def getAvgBlockSize(timespan: FiniteDuration): F[List[ChartPoint]]

  def getBlockChainSize(timespan: FiniteDuration): F[List[ChartPoint]]

  def getAvgDifficulty(timespan: FiniteDuration): F[List[ChartPoint]]

  def getAvgTxsNumPerBlock(timespan: FiniteDuration): F[List[ChartPoint]]

  def getTransactionsNum(timespan: FiniteDuration): F[List[ChartPoint]]

  def getMinersRevenue(timespan: FiniteDuration): F[List[ChartPoint]]

  def getHashRate(timespan: FiniteDuration): F[List[ChartPoint]]

  def getHashRateDistribution(timespan: FiniteDuration): F[List[HashRateDistributionSegment]]
}

object StatsService {

  def apply[
    F[_]: Clock: Sync,
    D[_]: LiftConnectionIO: Monad
  ](
    protocolSettings: ProtocolSettings
  )(trans: D Trans F): F[StatsService[F]] =
    (BlockInfoRepo[F, D], HeaderRepo[F, D], TransactionRepo[F, D], OutputRepo[F, D])
      .mapN(new Live(protocolSettings, _, _, _, _)(trans))

  final private class Live[F[_]: Clock: Functor: FlatMap, D[_]: Monad](
    protocolSettings: ProtocolSettings,
    blockInfoRepo: BlockInfoRepo[D],
    headerRepo: HeaderRepo[D],
    transactionRepo: TransactionRepo[D, Stream],
    outputRepo: OutputRepo[D, Stream]
  )(trans: D Trans F)
    extends StatsService[F] {

    def getCurrentStats: F[StatsSummary] =
      stats.getPastPeriodTsMillis.flatMap { ts =>
        (for {
          totalOuts <- outputRepo.sumOfAllUnspentOutputsSince(ts)
          estimatedOuts <- outputRepo
                            .estimatedOutputsSince(ts)(protocolSettings.genesisAddress)
          blocks <- blockInfoRepo.getManySince(ts)
        } yield stats.recentToStats(blocks, totalOuts, estimatedOuts)) ||> trans.xa
      }

    def getBlockChainInfo: F[BlockChainInfo] =
      stats.getPastPeriodTsMillis.flatMap { ts =>
        (for {
          headerOpt <- headerRepo.getLast
          diff      <- blockInfoRepo.totalDifficultySince(ts)
          hashRate = stats.dailyHashRate(diff)
          numTxs <- transactionRepo.countMainSince(ts)
          info = headerOpt.fold(BlockChainInfo.empty) { h =>
            val supply = protocolSettings.emission.issuedCoinsAfterHeight(h.height.toLong)
            BlockChainInfo(h.version.toString, supply, numTxs, hashRate)
          }
        } yield info) ||> trans.xa
      }

    def getTotalCoins(timespan: FiniteDuration): F[List[ChartPoint]] =
      shiftedTs(timespan)(
        blockInfoRepo.totalCoinsSince(_).map(_.map(ChartPoint.apply)) ||> trans.xa
      )

    def getAvgBlockSize(timespan: FiniteDuration): F[List[ChartPoint]] =
      shiftedTs(timespan)(
        blockInfoRepo.avgBlockSizeSince(_).map(_.map(ChartPoint.apply)) ||> trans.xa
      )

    def getBlockChainSize(timespan: FiniteDuration): F[List[ChartPoint]] =
      shiftedTs(timespan)(
        blockInfoRepo.totalBlockChainSizeSince(_).map(_.map(ChartPoint.apply)) ||> trans.xa
      )

    def getAvgDifficulty(timespan: FiniteDuration): F[List[ChartPoint]] =
      shiftedTs(timespan)(
        blockInfoRepo.avgDifficultiesSince(_).map(_.map(ChartPoint.apply)) ||> trans.xa
      )

    def getAvgTxsNumPerBlock(timespan: FiniteDuration): F[List[ChartPoint]] =
      shiftedTs(timespan)(
        blockInfoRepo.avgTxsQtySince(_).map(_.map(ChartPoint.apply)) ||> trans.xa
      )

    def getTransactionsNum(timespan: FiniteDuration): F[List[ChartPoint]] =
      shiftedTs(timespan)(
        blockInfoRepo.totalTxsQtySince(_).map(_.map(ChartPoint.apply)) ||> trans.xa
      )

    def getMinersRevenue(timespan: FiniteDuration): F[List[ChartPoint]] =
      shiftedTs(timespan)(
        blockInfoRepo.totalMinerRevenueSince(_).map(_.map(ChartPoint.apply)) ||> trans.xa
      )

    def getHashRate(timespan: FiniteDuration): F[List[ChartPoint]] =
      shiftedTs(timespan)(
        blockInfoRepo.totalDifficultiesSince(_).map(_.map(ChartPoint.apply)) ||> trans.xa
      )

    def getHashRateDistribution(timespan: FiniteDuration): F[List[HashRateDistributionSegment]] =
      shiftedTs(timespan)(
        blockInfoRepo.minerStatsSince(_).map(HashRateDistributionSegment.batch) ||> trans.xa
      )

    private def shiftedTs[G[_]: Clock: FlatMap, R](
      timespan: FiniteDuration
    )(fn: Long => G[R]): G[R] =
      Clock[G]
        .realTime(TimeUnit.MILLISECONDS)
        .flatMap(ts => fn(ts - timespan.toMillis))
  }
}
