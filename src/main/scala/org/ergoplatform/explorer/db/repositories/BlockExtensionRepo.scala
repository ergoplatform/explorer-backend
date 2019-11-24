package org.ergoplatform.explorer.db.repositories

import cats.implicits._
import doobie.free.implicits._
import doobie.refined.implicits._
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.BlockExtension
import org.ergoplatform.explorer.db.algebra.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.doobieInstances._

/** [[BlockExtension]] data access operations.
  */
trait BlockExtensionRepo[D[_]] {

  /** Put a given `extension` to persistence.
    */
  def insert(extension: BlockExtension): D[Unit]

  /** Get extension related to a given `headerId`.
    */
  def getByHeaderId(headerId: Id): D[Option[BlockExtension]]
}

object BlockExtensionRepo {

  def apply[D[_]: LiftConnectionIO]: BlockExtensionRepo[D] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO]
    extends BlockExtensionRepo[D] {

    import org.ergoplatform.explorer.db.queries.{BlockExtensionQuerySet => QS}

    def insert(extension: BlockExtension): D[Unit] =
      QS.insert(extension).void.liftConnectionIO

    def getByHeaderId(headerId: Id): D[Option[BlockExtension]] =
      QS.getByHeaderId(headerId).liftConnectionIO
  }
}
