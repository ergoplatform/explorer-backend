package org.ergoplatform.explorer.db.repositories

import cats.effect.Sync
import cats.syntax.functor._
import doobie.free.implicits._
import doobie.util.log.LogHandler
import eu.timepit.refined.refineMV
import org.ergoplatform.explorer.BlockId
import org.ergoplatform.explorer.constraints.{OrderingSpec, OrderingString}
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.BlockStats
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedBlockInfo, MinerStats, TimePoint}
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._

/** [[BlockStats]] data access operations.
  */
trait BlockInfoRepo[D[_]] {

  /** Put a given `blockInfo` to persistence.
    */
  def insert(blockInfo: BlockStats): D[Unit]

  /** Get block info with a given `headerId`.
    */
  def get(id: BlockId): D[Option[BlockStats]]

  /** Get slice of the main chain.
    */
  def getMany(
    offset: Int,
    limit: Int,
    order: OrderingString,
    sortBy: String
  ): D[List[ExtendedBlockInfo]]

  /** Stream blocks.
    */
  def stream(minGix: Long, limit: Int): fs2.Stream[D, ExtendedBlockInfo]

  /** Get all blocks appeared in the main chain after the given timestamp `ts`.
    */
  def getManySince(ts: Long): D[List[BlockStats]]

  /** Get all blocks with id matching a given `query`.
    */
  def getManyByIdLike(query: String): D[List[ExtendedBlockInfo]]

  /** Get size in bytes of the block with the given `id`.
    */
  def getBlockSize(id: BlockId): D[Option[Int]]

  def getLastStats: D[Option[BlockStats]]

  def totalDifficultySince(ts: Long): D[Long]

  def circulatingSupplySince(ts: Long): D[Long]

  def totalCoinsSince(ts: Long): D[List[TimePoint[Long]]]

  def avgBlockSizeSince(ts: Long): D[List[TimePoint[Long]]]

  def avgTxsQtySince(ts: Long): D[List[TimePoint[Long]]]

  def totalTxsQtySince(ts: Long): D[List[TimePoint[Long]]]

  def totalBlockChainSizeSince(ts: Long): D[List[TimePoint[Long]]]

  def avgDifficultiesSince(ts: Long): D[List[TimePoint[Long]]]

  def totalDifficultiesSince(ts: Long): D[List[TimePoint[Long]]]

  def totalMinerRevenueSince(ts: Long): D[List[TimePoint[Long]]]

  def minerStatsSince(ts: Long): D[List[MinerStats]]

  /** Update main_chain status for all inputs related to given `headerId`.
    */
  def updateChainStatusByHeaderId(headerId: BlockId, newChainStatus: Boolean): D[Unit]

  def updateTotalParamsByHeaderId(
    headerId: BlockId,
    newSize: Long,
    newTxsCount: Long,
    newMiningTime: Long,
    newFees: Long,
    newReward: Long,
    newCoins: Long
  ): D[Unit]
}

object BlockInfoRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[BlockInfoRepo[D]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler) extends BlockInfoRepo[D] {

    import org.ergoplatform.explorer.db.queries.{BlockInfoQuerySet => QS}

    def insert(blockInfo: BlockStats): D[Unit] =
      QS.insertNoConflict(blockInfo).void.liftConnectionIO

    def get(id: BlockId): D[Option[BlockStats]] =
      QS.getBlockInfo(id).option.liftConnectionIO

    def getMany(
      offset: Int,
      limit: Int,
      ordering: OrderingString,
      orderBy: String
    ): D[List[ExtendedBlockInfo]] =
      QS.getManyExtendedMain(offset, limit, ordering, orderBy).to[List].liftConnectionIO

    def stream(minGix: Long, limit: Int): fs2.Stream[D, ExtendedBlockInfo] =
      QS.getManyExtendedMain(minGix, limit, refineMV[OrderingSpec]("asc"), "height")
        .stream
        .translate(LiftConnectionIO[D].liftConnectionIOK)

    def getManySince(ts: Long): D[List[BlockStats]] =
      QS.getManySince(ts).to[List].liftConnectionIO

    def getManyByIdLike(query: String): D[List[ExtendedBlockInfo]] =
      QS.getManyExtendedByIdLike(query).to[List].liftConnectionIO

    def getBlockSize(id: BlockId): D[Option[Int]] =
      QS.getBlockSize(id).option.liftConnectionIO

    def getLastStats: D[Option[BlockStats]] =
      QS.getLastStats.option.liftConnectionIO

    def totalDifficultySince(ts: Long): D[Long] =
      QS.totalDifficultySince(ts).unique.liftConnectionIO

    def circulatingSupplySince(ts: Long): D[Long] =
      QS.circulatingSupplySince(ts).unique.liftConnectionIO

    def totalCoinsSince(ts: Long): D[List[TimePoint[Long]]] =
      QS.totalCoinsSince(ts).to[List].liftConnectionIO

    def avgBlockSizeSince(ts: Long): D[List[TimePoint[Long]]] =
      QS.avgBlockSizeSince(ts).to[List].liftConnectionIO

    def avgTxsQtySince(ts: Long): D[List[TimePoint[Long]]] =
      QS.avgTxsQtySince(ts).to[List].liftConnectionIO

    def totalTxsQtySince(ts: Long): D[List[TimePoint[Long]]] =
      QS.totalTxsQtySince(ts).to[List].liftConnectionIO

    def totalBlockChainSizeSince(ts: Long): D[List[TimePoint[Long]]] =
      QS.totalBlockChainSizeSince(ts).to[List].liftConnectionIO

    def avgDifficultiesSince(ts: Long): D[List[TimePoint[Long]]] =
      QS.avgDifficultiesSince(ts).to[List].liftConnectionIO

    def totalDifficultiesSince(ts: Long): D[List[TimePoint[Long]]] =
      QS.totalDifficultiesSince(ts).to[List].liftConnectionIO

    def totalMinerRevenueSince(ts: Long): D[List[TimePoint[Long]]] =
      QS.totalMinerRevenueSince(ts).to[List].liftConnectionIO

    def minerStatsSince(ts: Long): D[List[MinerStats]] =
      QS.minerStatsSince(ts).to[List].liftConnectionIO

    def updateChainStatusByHeaderId(headerId: BlockId, newChainStatus: Boolean): D[Unit] =
      QS.updateChainStatusByHeaderId(headerId, newChainStatus).run.void.liftConnectionIO

    def updateTotalParamsByHeaderId(
      headerId: BlockId,
      newSize: Long,
      newTxsCount: Long,
      newMiningTime: Long,
      newFees: Long,
      newReward: Long,
      newCoins: Long
    ): D[Unit] =
      QS.updateTotalParametersByHeaderId(headerId, newSize, newTxsCount, newMiningTime, newFees, newReward, newCoins)
        .run
        .void
        .liftConnectionIO
  }
}
