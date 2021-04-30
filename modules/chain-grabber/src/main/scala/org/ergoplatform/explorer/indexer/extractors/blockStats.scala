package org.ergoplatform.explorer.indexer.extractors

import cats.Functor
import org.ergoplatform.explorer.db.models.BlockStats
import org.ergoplatform.explorer.indexer.models.TotalParams
import org.ergoplatform.explorer.settings.ProtocolSettings
import tofu.WithContext
import tofu.syntax.context._
import tofu.syntax.monadic._

object blockStats {

  @inline def produceTotalParams[F[_]: Functor: WithContext[*[_], ProtocolSettings]](
    currentBlockStats: BlockStats,
    prevBlockStats: BlockStats
  ): F[TotalParams] =
    context.map { protocolSettings =>
      val correctTotalBlockchainSize = prevBlockStats.blockChainTotalSize + currentBlockStats.blockSize
      val correctTotalTxsCount       = prevBlockStats.totalTxsCount + currentBlockStats.txsCount
      val totalCoinIssued            = protocolSettings.emission.issuedCoinsAfterHeight(currentBlockStats.height)
      val blockMiningTime            = currentBlockStats.timestamp - prevBlockStats.timestamp
      val correctTotalMiningTime     = prevBlockStats.totalMiningTime + blockMiningTime
      val correctTotalFees           = prevBlockStats.totalFees + currentBlockStats.blockFee
      val correctMinerReward         = prevBlockStats.totalMinersReward + currentBlockStats.minerReward
      val correctTotalCoins          = prevBlockStats.totalCoinsInTxs + currentBlockStats.blockCoins
      TotalParams(
        correctTotalBlockchainSize,
        correctTotalTxsCount,
        totalCoinIssued,
        correctTotalMiningTime,
        correctTotalFees,
        correctMinerReward,
        correctTotalCoins
      )
    }
}
