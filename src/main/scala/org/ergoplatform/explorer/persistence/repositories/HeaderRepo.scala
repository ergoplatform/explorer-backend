package org.ergoplatform.explorer.persistence.repositories

import cats.effect.Sync
import cats.implicits._
import doobie.util.transactor.Transactor
import doobie.implicits._
import doobie.refined.implicits._
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.persistence.models.Header

/** [[Header]] data access operations.
  */
trait HeaderRepo[F[_]] {

  /** Put a given `h` to persistence.
    */
  def insert(h: Header): F[Unit]

  /** Update a header with a given `h.id` according to a new values from `h`.
    */
  def update(h: Header): F[Unit]

  /** Get header with a given `id`.
    */
  def get(id: Id): F[Option[Header]]

  /** Get all headers at the given `height`.
    */
  def getAllByHeight(height: Int): F[List[Header]]

  /** Get height of a header with a given `id`.
    */
  def getHeightOf(id: Id): F[Option[Int]]
}

object HeaderRepo {

  final class Live[F[_]: Sync](xa: Transactor[F]) extends HeaderRepo[F] {

    import org.ergoplatform.explorer.persistence.queries.{HeaderQuerySet => QS}

    def insert(h: Header): F[Unit] =
      QS.insert(h).transact(xa).as(())

    def update(h: Header): F[Unit] =
      QS.update(h).transact(xa).as(())

    def get(id: Id): F[Option[Header]] =
      QS.get(id).transact(xa)

    def getAllByHeight(height: Int): F[List[Header]] =
      QS.getAllByHeight(height).transact(xa)

    def getHeightOf(id: Id): F[Option[Int]] =
      QS.getHeightOf(id).transact(xa)
  }
}
