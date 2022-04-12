package org.ergoplatform.explorer.db.repositories

import cats.effect.Sync
import cats.implicits._
import doobie.free.implicits._
import doobie.refined.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.explorer.BlockId
import org.ergoplatform.explorer.constraints.OrderingString
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
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
  def get(id: BlockId): D[Option[Header]]

  /** Get last header in the chain.
    */
  def getLast: D[Option[Header]]

  /** Get header with a given `parentId`.
    */
  def getByParentId(parentId: BlockId): D[Option[Header]]

  /** Get all headers at the given `height`.
    */
  def getAllByHeight(height: Int): D[List[Header]]

  /** Get height of a header with a given `id`.
    */
  def getHeightOf(id: BlockId): D[Option[Int]]

  /** Get height of the best known header.
    */
  def getBestHeight: D[Int]

  /** Update main chain flag with a given `newChainStatus`
    * for a header with a given `id`.
    */
  def updateChainStatusById(id: BlockId, newChainStatus: Boolean): D[Unit]

  /** Get slice of the main chain.
    */
  def getMany(
    offset: Int,
    limit: Int,
    order: OrderingString,
    sortBy: String
  ): D[List[Header]]
}

object HeaderRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[HeaderRepo[D]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler) extends HeaderRepo[D] {

    import org.ergoplatform.explorer.db.queries.{HeaderQuerySet => QS}

    def insert(h: Header): D[Unit] =
      QS.insertNoConflict(h).void.liftConnectionIO

    def get(id: BlockId): D[Option[Header]] =
      QS.get(id).option.liftConnectionIO

    def getLast: D[Option[Header]] =
      QS.getLast.option.liftConnectionIO

    def getByParentId(parentId: BlockId): D[Option[Header]] =
      QS.getByParentId(parentId).option.liftConnectionIO

    def getAllByHeight(height: Int): D[List[Header]] =
      QS.getAllByHeight(height).to[List].liftConnectionIO

    def getHeightOf(id: BlockId): D[Option[Int]] =
      QS.getHeightOf(id).option.liftConnectionIO

    def getBestHeight: D[Int] =
      QS.getBestHeight.option
        .map(_.getOrElse(constants.PreGenesisHeight))
        .liftConnectionIO

    def updateChainStatusById(id: BlockId, newChainStatus: Boolean): D[Unit] =
      QS.updateChainStatusById(id, newChainStatus).run.void.liftConnectionIO

    def getMany(
      offset: Int,
      limit: Int,
      ordering: OrderingString,
      orderBy: String
    ): D[List[Header]] =
      QS.getMany(offset, limit, ordering, orderBy).to[List].liftConnectionIO
  }
}
