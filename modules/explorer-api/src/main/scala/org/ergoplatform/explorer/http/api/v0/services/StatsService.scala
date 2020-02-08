package org.ergoplatform.explorer.http.api.v0.services

import cats.~>
import fs2.Stream
import org.ergoplatform.explorer.db.repositories.BlockInfoRepo
import org.ergoplatform.explorer.http.api.v0.models.{BlockChainInfo, StatsSummary}

trait StatsService[F[_]] {

  /** Get blockchain statistics summary.
    */
  def getCurrentStats: F[StatsSummary]

  /** Get short blockchain info.
    */
  def getBlockchainInfo: F[BlockChainInfo]
}

object StatsService {

  final private class Live[F[_], D[_]](blockInfoRepo: BlockInfoRepo[D, Stream])(
    xa: D ~> F
  ) extends StatsService[F] {

    def getCurrentStats: F[StatsSummary] = ???

    def getBlockchainInfo: F[BlockChainInfo] = ???
  }
}
