package org.ergoplatform.explorer.http.api.v0.services

import java.util.concurrent.TimeUnit

import cats.effect.Clock
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{~>, FlatMap, Functor, Monad}
import fs2.Stream
import mouse.anyf._
import org.ergoplatform.explorer.db.repositories.{BlockInfoRepo, OutputRepo}
import org.ergoplatform.explorer.http.api.v0.domain.stats
import org.ergoplatform.explorer.http.api.v0.models.{BlockChainInfo, StatsSummary}
import org.ergoplatform.explorer.settings.ProtocolSettings

trait StatsService[F[_]] {

  /** Get blockchain statistics summary.
    */
  def getCurrentStats: F[StatsSummary]

  /** Get short blockchain info.
    */
  def getBlockchainInfo: F[BlockChainInfo]
}

object StatsService {

  final private class Live[F[_]: Clock: Functor: FlatMap, D[_]: Monad](
    protocolSettings: ProtocolSettings,
    blockInfoRepo: BlockInfoRepo[D, Stream],
    outputRepo: OutputRepo[D, Stream]
  )(
    xa: D ~> F
  ) extends StatsService[F] {

    def getCurrentStats: F[StatsSummary] =
      stats.getPastTs.flatMap { ts =>
        (for {
          totalOuts <- outputRepo.sumOfAllUnspentOutputsSince(ts)
          estimatedOuts <- outputRepo
                            .estimatedOutputsSince(ts)(protocolSettings.genesisAddress)
          blocks <- blockInfoRepo.getManySince(ts)
        } yield stats.recentToStats(blocks, totalOuts, estimatedOuts)) ||> xa
      }

    def getBlockchainInfo: F[BlockChainInfo] = ???
  }
}
