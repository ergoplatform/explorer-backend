package org.ergoplatform.explorer.persistence.repositories

import cats.effect.Sync
import cats.implicits._
import doobie.util.transactor.Transactor
import doobie.implicits._
import doobie.refined.implicits._
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.persistence.models.BlockExtension
import org.ergoplatform.explorer.persistence.doobieInstances._

/** [[BlockExtension]] data access operations.
  */
trait BlockExtensionRepo[F[_]] {

  /** Put a given `extension` to persistence.
    */
  def insert(extension: BlockExtension): F[Unit]

  /** Get extension related to a given `headerId`.
    */
  def getByHeaderId(headerId: Id): F[Option[BlockExtension]]
}

object BlockExtensionRepo {

  final class Live[F[_]: Sync](xa: Transactor[F])
    extends BlockExtensionRepo[F] {

    import org.ergoplatform.explorer.persistence.queries.{BlockExtensionQuerySet => QS}

    def insert(extension: BlockExtension): F[Unit] =
      QS.insert(extension).transact(xa).as(())

    def getByHeaderId(headerId: Id): F[Option[BlockExtension]] =
      QS.getByHeaderId(headerId).transact(xa)
  }
}
