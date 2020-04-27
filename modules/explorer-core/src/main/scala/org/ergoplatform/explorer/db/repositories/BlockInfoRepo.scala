package org.ergoplatform.explorer.db.repositories

import cats.effect.Sync
import cats.syntax.functor._
import doobie.free.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.constraints.OrderingString
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.LiftConnectionIO
import org.ergoplatform.explorer.db.syntax.liftConnIO._
import org.ergoplatform.explorer.db.models.BlockInfo
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedBlockInfo, MinerStats, TimePoint}

/** [[BlockInfo]] data access operations.
  */
trait BlockInfoRepo[D[_]] {

  /** Put a given `blockInfo` to persistence.
    */
  def insert(blockInfo: BlockInfo): D[Unit]

  /** Get block info with a given `headerId`.
    */
  def get(id: Id): D[Option[BlockInfo]]

  /** Get slice of the main chain.
    */
  def getMany(
    offset: Int,
    limit: Int,
    order: OrderingString,
    sortBy: String
  ): D[List[ExtendedBlockInfo]]

  /** Get all blocks appeared in the main chain after the given timestamp `ts`.
    */
  def getManySince(ts: Long): D[List[BlockInfo]]

  /** Get all blocks with id matching a given `query`.
    */
  def getManyByIdLike(query: String): D[List[ExtendedBlockInfo]]

  /** Get size in bytes of the block with the given `id`.
    */
  def getBlockSize(id: Id): D[Option[Int]]

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
}

object BlockInfoRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[BlockInfoRepo[D]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler)
    extends BlockInfoRepo[D] {

    import org.ergoplatform.explorer.db.queries.{BlockInfoQuerySet => QS}

    def insert(blockInfo: BlockInfo): D[Unit] =
      QS.insert(blockInfo).void.liftConnIO

    def get(id: Id): D[Option[BlockInfo]] =
      QS.getBlockInfo(id).option.liftConnIO

    def getMany(
      offset: Int,
      limit: Int,
      ordering: OrderingString,
      orderBy: String
    ): D[List[ExtendedBlockInfo]] =
      QS.getManyExtendedMain(offset, limit, ordering, orderBy).to[List].liftConnIO

    def getManySince(ts: Long): D[List[BlockInfo]] =
      QS.getManySince(ts).to[List].liftConnIO

    def getManyByIdLike(query: String): D[List[ExtendedBlockInfo]] =
      QS.getManyExtendedByIdLike(query).to[List].liftConnIO

    def getBlockSize(id: Id): D[Option[Int]] =
      QS.getBlockSize(id).option.liftConnIO

    def totalDifficultySince(ts: Long): D[Long] =
      QS.totalDifficultySince(ts).unique.liftConnIO

    def circulatingSupplySince(ts: Long): D[Long] =
      QS.circulatingSupplySince(ts).unique.liftConnIO

    def totalCoinsSince(ts: Long): D[List[TimePoint[Long]]] =
      QS.totalCoinsSince(ts).to[List].liftConnIO

    def avgBlockSizeSince(ts: Long): D[List[TimePoint[Long]]] =
      QS.avgBlockSizeSince(ts).to[List].liftConnIO

    def avgTxsQtySince(ts: Long): D[List[TimePoint[Long]]] =
      QS.avgTxsQtySince(ts).to[List].liftConnIO

    def totalTxsQtySince(ts: Long): D[List[TimePoint[Long]]] =
      QS.totalTxsQtySince(ts).to[List].liftConnIO

    def totalBlockChainSizeSince(ts: Long): D[List[TimePoint[Long]]] =
      QS.totalBlockChainSizeSince(ts).to[List].liftConnIO

    def avgDifficultiesSince(ts: Long): D[List[TimePoint[Long]]] =
      QS.avgDifficultiesSince(ts).to[List].liftConnIO

    def totalDifficultiesSince(ts: Long): D[List[TimePoint[Long]]] =
      QS.totalDifficultiesSince(ts).to[List].liftConnIO

    def totalMinerRevenueSince(ts: Long): D[List[TimePoint[Long]]] =
      QS.totalMinerRevenueSince(ts).to[List].liftConnIO

    def minerStatsSince(ts: Long): D[List[MinerStats]] =
      QS.minerStatsSince(ts).to[List].liftConnIO
  }
}
