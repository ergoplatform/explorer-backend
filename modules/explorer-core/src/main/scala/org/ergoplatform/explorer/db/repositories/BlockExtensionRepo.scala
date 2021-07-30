package org.ergoplatform.explorer.db.repositories

import cats.effect.Sync
import cats.implicits._
import doobie.free.implicits._
import doobie.refined.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.explorer.BlockId
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.BlockExtension
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.doobieInstances._

/** [[BlockExtension]] data access operations.
  */
trait BlockExtensionRepo[D[_]] {

  /** Put a given `extension` to persistence.
    */
  def insert(extension: BlockExtension): D[Unit]

  /** Get extension related to a given `headerId`.
    */
  def getByHeaderId(headerId: BlockId): D[Option[BlockExtension]]
}

object BlockExtensionRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[BlockExtensionRepo[D]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler)
    extends BlockExtensionRepo[D] {

    import org.ergoplatform.explorer.db.queries.{BlockExtensionQuerySet => QS}

    def insert(extension: BlockExtension): D[Unit] =
      QS.insertNoConflict(extension).void.liftConnectionIO

    def getByHeaderId(headerId: BlockId): D[Option[BlockExtension]] =
      QS.getByHeaderId(headerId).option.liftConnectionIO
  }
}
