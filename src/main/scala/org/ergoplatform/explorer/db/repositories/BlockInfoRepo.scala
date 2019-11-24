package org.ergoplatform.explorer.db.repositories

import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.BlockInfo

trait BlockInfoRepo[D[_]] {

  def getByHeaderId(headerId: Id): D[Option[BlockInfo]]
}

object BlockInfoRepo {

  def apply[D[_]: LiftConnectionIO]: BlockInfoRepo[D] =
    new Live[D]

  private final class Live[D[_]: LiftConnectionIO] extends BlockInfoRepo[D] {

    def getByHeaderId(headerId: Id): D[Option[BlockInfo]] = ???
  }
}
