package org.ergoplatform.explorer.http.api.v0.services

import cats.effect.{Clock, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.apply._
import cats.{FlatMap, Functor, Monad}
import fs2.Stream
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import mouse.anyf._
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories.{BlockInfoRepo, HeaderRepo, OutputRepo, TransactionRepo}
import org.ergoplatform.explorer.http.api.v0.domain.stats
import org.ergoplatform.explorer.http.api.v0.models.{BlockChainInfo, StatsSummary}
import org.ergoplatform.explorer.settings.ProtocolSettings

trait StatsService[F[_]] {

  /** Get blockchain statistics summary.
    */
  def getCurrentStats: F[StatsSummary]

  /** Get short blockchain info.
    */
  def getBlockChainInfo: F[BlockChainInfo]
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
    blockInfoRepo: BlockInfoRepo[D, Stream],
    headerRepo: HeaderRepo[D],
    transactionRepo: TransactionRepo[D, Stream],
    outputRepo: OutputRepo[D, Stream]
  )(
    trans: D Trans F
  ) extends StatsService[F] {

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
  }
}
