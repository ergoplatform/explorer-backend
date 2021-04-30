package org.ergoplatform.explorer.indexer.extractors

import cats.Functor
import org.ergoplatform.explorer.db.models.BlockStats
import org.ergoplatform.explorer.indexer.models.TotalStats
import org.ergoplatform.explorer.settings.ProtocolSettings
import tofu.WithContext
import tofu.syntax.context._
import tofu.syntax.monadic._

object blockStats {

  @inline def recalculateStats[F[_]: Functor: WithContext[*[_], ProtocolSettings]](
    currentBlockStats: BlockStats,
    prevBlockStats: BlockStats
  ): F[TotalStats] =
    context.map { protocolSettings =>
      val talBlockchainSize = prevBlockStats.blockChainTotalSize + currentBlockStats.blockSize
      val totalTxsCount     = prevBlockStats.totalTxsCount + currentBlockStats.txsCount
      val coinIssued        = protocolSettings.emission.issuedCoinsAfterHeight(currentBlockStats.height)
      val blockMiningTime   = currentBlockStats.timestamp - prevBlockStats.timestamp
      val totalMiningTime   = prevBlockStats.totalMiningTime + blockMiningTime
      val totalFees         = prevBlockStats.totalFees + currentBlockStats.blockFee
      val minerReward       = prevBlockStats.totalMinersReward + currentBlockStats.minerReward
      val totalCoins        = prevBlockStats.totalCoinsInTxs + currentBlockStats.blockCoins
      TotalStats(
        talBlockchainSize,
        totalTxsCount,
        coinIssued,
        totalMiningTime,
        totalFees,
        minerReward,
        totalCoins
      )
    }
}
