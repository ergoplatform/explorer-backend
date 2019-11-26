package org.ergoplatform.explorer.db.repositories

import cats.syntax.functor._
import doobie.free.implicits._
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.algebra.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.models.BlockInfo

/** [[BlockInfo]] data access operations.
  */
trait BlockInfoRepo[D[_]] {

  /** Put a given `blockInfo` to persistence.
    */
  def insert(blockInfo: BlockInfo): D[Unit]

  /** Get block info with a given `headerId`.
    */
  def getByHeaderId(headerId: Id): D[Option[BlockInfo]]
}

object BlockInfoRepo {

  def apply[D[_]: LiftConnectionIO]: BlockInfoRepo[D] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO] extends BlockInfoRepo[D] {

    import org.ergoplatform.explorer.db.queries.{BlockInfoQuerySet => QS}

    def insert(blockInfo: BlockInfo): D[Unit] =
      QS.insert(blockInfo).void.liftConnectionIO

    def getByHeaderId(headerId: Id): D[Option[BlockInfo]] =
      QS.getBlockInfo(headerId).liftConnectionIO
  }
}
