package org.ergoplatform.explorer.db.repositories

import cats.syntax.functor._
import doobie.free.implicits._
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.models.BlockInfo

/** [[BlockInfo]] data access operations.
  */
trait BlockInfoRepo[D[_], S[_[_], _]] {

  /** Put a given `blockInfo` to persistence.
    */
  def insert(blockInfo: BlockInfo): D[Unit]

  /** Get block info with a given `headerId`.
    */
  def get(id: Id): D[Option[BlockInfo]]

  /** Get slice of the main chain.
    */
  def getMany(offset: Int, limit: Int): S[D, BlockInfo]

  /** Get size in bytes of the block with the given `id`.
    */
  def getBlockSize(id: Id): D[Option[Int]]
}

object BlockInfoRepo {

  def apply[D[_]: LiftConnectionIO]: BlockInfoRepo[D, fs2.Stream] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO] extends BlockInfoRepo[D, fs2.Stream] {

    import org.ergoplatform.explorer.db.queries.{BlockInfoQuerySet => QS}

    def insert(blockInfo: BlockInfo): D[Unit] =
      QS.insert(blockInfo).void.liftConnectionIO

    def get(id: Id): D[Option[BlockInfo]] =
      QS.getBlockInfo(id).option.liftConnectionIO

    def getMany(offset: Int, limit: Int): fs2.Stream[D, BlockInfo] =
      QS.getMany(offset, limit).stream.translate(LiftConnectionIO[D].liftConnectionIOK)

    def getBlockSize(id: Id): D[Option[Int]] =
      QS.getBlockSize(id).option.liftConnectionIO
  }
}
