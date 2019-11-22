package org.ergoplatform.explorer.db.repositories

import cats.Functor
import cats.implicits._
import doobie.refined.implicits._
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.algebra.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.models.Header

/** [[Header]] data access operations.
  */
trait HeaderRepo[D[_]] {

  /** Put a given `h` to persistence.
    */
  def insert(h: Header): D[Unit]

  /** Get header with a given `id`.
    */
  def get(id: Id): D[Option[Header]]

  /** Get all headers at the given `height`.
    */
  def getAllByHeight(height: Int): D[List[Header]]

  /** Get height of a header with a given `id`.
    */
  def getHeightOf(id: Id): D[Option[Int]]
}

object HeaderRepo {

  def apply[D[_]: LiftConnectionIO: Functor]: HeaderRepo[D] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO: Functor] extends HeaderRepo[D] {

    import org.ergoplatform.explorer.db.queries.{HeaderQuerySet => QS}

    def insert(h: Header): D[Unit] =
      QS.insert(h).liftConnectionIO.void

    def get(id: Id): D[Option[Header]] =
      QS.get(id).liftConnectionIO

    def getAllByHeight(height: Int): D[List[Header]] =
      QS.getAllByHeight(height).liftConnectionIO

    def getHeightOf(id: Id): D[Option[Int]] =
      QS.getHeightOf(id).liftConnectionIO
  }
}
