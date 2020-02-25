package org.ergoplatform.explorer.http.api.v0.services

import cats.effect.Clock
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{~>, FlatMap, Functor, Monad}
import fs2.Stream
import mouse.anyf._
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories.{
  BlockInfoRepo,
  HeaderRepo,
  OutputRepo,
  TransactionRepo
}
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
    F[_]: Clock: Functor: FlatMap,
    D[_]: LiftConnectionIO: Monad
  ](
    protocolSettings: ProtocolSettings
  )(xa: D ~> F): StatsService[F] =
    new Live(
      protocolSettings,
      BlockInfoRepo[D],
      HeaderRepo[D],
      TransactionRepo[D],
      OutputRepo[D]
    )(xa)

  final private class Live[F[_]: Clock: Functor: FlatMap, D[_]: Monad](
    protocolSettings: ProtocolSettings,
    blockInfoRepo: BlockInfoRepo[D, Stream],
    headerRepo: HeaderRepo[D],
    transactionRepo: TransactionRepo[D, Stream],
    outputRepo: OutputRepo[D, Stream]
  )(
    xa: D ~> F
  ) extends StatsService[F] {

    def getCurrentStats: F[StatsSummary] =
      stats.getPastPeriodTsMillis.flatMap { ts =>
        (for {
          totalOuts <- outputRepo.sumOfAllUnspentOutputsSince(ts)
          estimatedOuts <- outputRepo
                            .estimatedOutputsSince(ts)(protocolSettings.genesisAddress)
          blocks <- blockInfoRepo.getManySince(ts)
        } yield stats.recentToStats(blocks, totalOuts, estimatedOuts)) ||> xa
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
        } yield info) ||> xa
      }
  }
}
