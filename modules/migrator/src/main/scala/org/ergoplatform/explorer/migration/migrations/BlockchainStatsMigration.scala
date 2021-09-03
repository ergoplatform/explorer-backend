package org.ergoplatform.explorer.migration.migrations

import cats.Parallel
import cats.effect.{IO, Timer}
import doobie.ConnectionIO
import cats.syntax.traverse._
import cats.syntax.option._
import doobie.util.transactor.Transactor
import doobie.ConnectionIO
import doobie.implicits._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.explorer.BlockId
import org.ergoplatform.explorer.db.models.BlockStats
import org.ergoplatform.explorer.db.models.aggregates.ExtendedBlockInfo
import org.ergoplatform.explorer.db.repositories.BlockInfoRepo
import org.ergoplatform.explorer.http.api.models.Sorting.Asc
import org.ergoplatform.explorer.migration.configs.ProcessingConfig

final class BlockchainStatsMigration(
  conf: ProcessingConfig,
  blockInfoRepo: BlockInfoRepo[ConnectionIO],
  xa: Transactor[IO],
  log: Logger[IO]
) {

  def run: IO[Unit] = migrateBatch(conf.offset, conf.batchSize)

  def migrateBatch(offset: Int, limit: Int): IO[Unit] =
    for {
      _    <- log.info(s"Current offset is [$offset]")
      data <- blockInfoRepo.getMany(offset, limit, Asc.value, "height").transact(xa)
      (correctHeights, _) = data.foldLeft((List.empty[(BlockId, BlockStats)], data.headOption)) {
                              case ((acc, Some(prevBlock)), currentBlock)
                                  if currentBlock.blockInfo.blockChainTotalSize < prevBlock.blockInfo.blockChainTotalSize =>
                                val blockMiningTime = currentBlock.blockInfo.timestamp - prevBlock.blockInfo.timestamp
                                val newBlockStats = currentBlock.blockInfo.copy(
                                  blockChainTotalSize =
                                    prevBlock.blockInfo.blockChainTotalSize + currentBlock.blockInfo.blockSize,
                                  totalTxsCount   = prevBlock.blockInfo.totalTxsCount + currentBlock.blockInfo.txsCount,
                                  totalMiningTime = prevBlock.blockInfo.totalMiningTime + blockMiningTime,
                                  totalFees       = prevBlock.blockInfo.totalFees + currentBlock.blockInfo.blockFee,
                                  totalMinersReward =
                                    prevBlock.blockInfo.totalMinersReward + currentBlock.blockInfo.minerReward,
                                  totalCoinsInTxs =
                                    prevBlock.blockInfo.totalCoinsInTxs + currentBlock.blockInfo.blockCoins
                                )
                                (acc :+ (currentBlock.blockInfo.headerId -> newBlockStats)) -> ExtendedBlockInfo(
                                  currentBlock.blockVersion,
                                  newBlockStats,
                                  currentBlock.minerNameOpt
                                ).some
                              case ((acc, _), currentBlock) => acc -> currentBlock.some
                            }
      _ <- correctHeights.traverse { case (id, newInfo) =>
             blockInfoRepo
               .updateTotalParamsByHeaderId(
                 id,
                 newInfo.blockChainTotalSize,
                 newInfo.totalTxsCount,
                 newInfo.totalMiningTime,
                 newInfo.totalFees,
                 newInfo.totalMinersReward,
                 newInfo.totalCoinsInTxs
               )
               .transact(xa)
           }
      _ <- if (data.nonEmpty) migrateBatch((offset + limit - 1), limit) else IO.unit
    } yield ()
}

object BlockchainStatsMigration {

  def apply(
    conf: ProcessingConfig,
    xa: Transactor[IO]
  )(implicit timer: Timer[IO], par: Parallel[IO]): IO[Unit] =
    for {
      logger <- Slf4jLogger.create[IO]
      bi     <- BlockInfoRepo[IO, ConnectionIO]
      _      <- new BlockchainStatsMigration(conf, bi, xa, logger).run
    } yield ()
}
