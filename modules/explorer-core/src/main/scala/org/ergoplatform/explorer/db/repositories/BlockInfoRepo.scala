package org.ergoplatform.explorer.db.repositories

import cats.syntax.functor._
import doobie.free.implicits._
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.models.BlockInfo
import org.ergoplatform.explorer.db.models.aggregates.{ChartPoint, ExtendedBlockInfo}

/** [[BlockInfo]] data access operations.
  */
trait BlockInfoRepo[D[_], S[_[_], _]] {

  /** Put a given `blockInfo` to persistence.
    */
  def insert(blockInfo: BlockInfo): D[Unit]

  /** Get block info with a given `headerId`.
    */
  def get(id: Id): D[Option[ExtendedBlockInfo]]

  /** Get slice of the main chain.
    */
  def getMany(offset: Int, limit: Int): S[D, ExtendedBlockInfo]

  /** Get all blocks appeared in the main chain after the given timestamp `ts`.
    */
  def getManySince(ts: Long): D[List[BlockInfo]]

  /** Get size in bytes of the block with the given `id`.
    */
  def getBlockSize(id: Id): D[Option[Int]]

  def totalDifficultySince(ts: Long): D[Long]

  def circulatingSupplySince(ts: Long): D[Long]

  def totalCoinsSince(ts: Long): D[List[ChartPoint]]

  def avgBlockSizeSince(ts: Long): D[List[ChartPoint]]

  def avgTxsQtySince(ts: Long): D[List[ChartPoint]]

  def totalTxsQtySince(ts: Long): D[List[ChartPoint]]

  def totalBlockChainSizeSince(ts: Long): D[List[ChartPoint]]

  def avgDifficultiesSince(ts: Long): D[List[ChartPoint]]

  def totalDifficultiesSince(ts: Long): D[List[ChartPoint]]

  def totalMinerRevenueSince(ts: Long): D[List[ChartPoint]]
}

object BlockInfoRepo {

  def apply[D[_]: LiftConnectionIO]: BlockInfoRepo[D, fs2.Stream] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO] extends BlockInfoRepo[D, fs2.Stream] {

    import org.ergoplatform.explorer.db.queries.{BlockInfoQuerySet => QS}

    def insert(blockInfo: BlockInfo): D[Unit] =
      QS.insert(blockInfo).void.liftConnectionIO

    def get(id: Id): D[Option[ExtendedBlockInfo]] =
      QS.getBlockInfo(id).option.liftConnectionIO

    def getMany(offset: Int, limit: Int): fs2.Stream[D, ExtendedBlockInfo] =
      QS.getMany(offset, limit).stream.translate(LiftConnectionIO[D].liftConnectionIOK)

    def getManySince(ts: Long): D[List[BlockInfo]] =
      QS.getManySince(ts).to[List].liftConnectionIO

    def getBlockSize(id: Id): D[Option[Int]] =
      QS.getBlockSize(id).option.liftConnectionIO

    def totalDifficultySince(ts: Long): D[Long] =
      QS.totalDifficultySince(ts).unique.liftConnectionIO

    def circulatingSupplySince(ts: Long): D[Long] =
      QS.circulatingSupplySince(ts).unique.liftConnectionIO

    def totalCoinsSince(ts: Long): D[List[ChartPoint]] =
      QS.totalCoinsSince(ts).to[List].liftConnectionIO

    def avgBlockSizeSince(ts: Long): D[List[ChartPoint]] =
      QS.avgBlockSizeSince(ts).to[List].liftConnectionIO

    def avgTxsQtySince(ts: Long): D[List[ChartPoint]] =
      QS.avgTxsQtySince(ts).to[List].liftConnectionIO

    def totalTxsQtySince(ts: Long): D[List[ChartPoint]] =
      QS.totalTxsQtySince(ts).to[List].liftConnectionIO

    def totalBlockChainSizeSince(ts: Long): D[List[ChartPoint]] =
      QS.totalBlockChainSizeSince(ts).to[List].liftConnectionIO

    def avgDifficultiesSince(ts: Long): D[List[ChartPoint]] =
      QS.avgDifficultiesSince(ts).to[List].liftConnectionIO

    def totalDifficultiesSince(ts: Long): D[List[ChartPoint]] =
      QS.totalDifficultiesSince(ts).to[List].liftConnectionIO

    def totalMinerRevenueSince(ts: Long): D[List[ChartPoint]] =
      QS.totalMinerRevenueSince(ts).to[List].liftConnectionIO
  }
}
