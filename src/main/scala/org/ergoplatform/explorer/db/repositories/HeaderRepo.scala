package org.ergoplatform.explorer.db.repositories

import cats.implicits._
import doobie.free.implicits._
import doobie.refined.implicits._
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.algebra.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.models.Header
import org.ergoplatform.explorer.protocol.constants

/** [[Header]] data access operations.
  */
trait HeaderRepo[D[_]] {

  /** Put a given `h` to persistence.
    */
  def insert(h: Header): D[Unit]

  /** Get header with a given `id`.
    */
  def get(id: Id): D[Option[Header]]

  /** Get header with a given `parentId`.
    */
  def getByParentId(parentId: Id): D[Option[Header]]

  /** Get all headers at the given `height`.
    */
  def getAllByHeight(height: Int): D[List[Header]]

  /** Get height of a header with a given `id`.
    */
  def getHeightOf(id: Id): D[Option[Int]]

  /** Get height of the best known header.
    */
  def getBestHeight: D[Int]

  /** Update main chain flag with a given `newChainStatus`
    * for a header with a given `id`.
    */
  def updateChainStatusById(id: Id, newChainStatus: Boolean): D[Unit]
}

object HeaderRepo {

  def apply[D[_]: LiftConnectionIO]: HeaderRepo[D] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO] extends HeaderRepo[D] {

    import org.ergoplatform.explorer.db.queries.{HeaderQuerySet => QS}

    def insert(h: Header): D[Unit] =
      QS.insert(h).void.liftConnectionIO

    def get(id: Id): D[Option[Header]] =
      QS.get(id).liftConnectionIO

    def getByParentId(parentId: Id): D[Option[Header]] = ???

    def getAllByHeight(height: Int): D[List[Header]] =
      QS.getAllByHeight(height).liftConnectionIO

    def getHeightOf(id: Id): D[Option[Int]] =
      QS.getHeightOf(id).liftConnectionIO

    def getBestHeight: D[Int] =
      QS.getBestHeight
        .map(_.getOrElse(constants.PreGenesisHeight))
        .liftConnectionIO

    def updateChainStatusById(id: Id, newChainStatus: Boolean): D[Unit] =
      QS.updateChainStatusById(id, newChainStatus).void.liftConnectionIO
  }
}
