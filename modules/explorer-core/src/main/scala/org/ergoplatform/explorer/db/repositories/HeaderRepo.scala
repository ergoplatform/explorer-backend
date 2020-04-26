package org.ergoplatform.explorer.db.repositories

import cats.effect.Sync
import cats.implicits._
import derevo.derive
import doobie.free.implicits._
import doobie.refined.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.LiftConnectionIO
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.models.Header
import org.ergoplatform.explorer.protocol.constants
import tofu.data.derived.ContextEmbed
import tofu.higherKind.derived.embed

/** [[Header]] data access operations.
  */
@derive(embed) trait HeaderRepo[D[_]] {

  /** Put a given `h` to persistence.
    */
  def insert(h: Header): D[Unit]

  /** Get header with a given `id`.
    */
  def get(id: Id): D[Option[Header]]

  /** Get last header in the chain.
    */
  def getLast: D[Option[Header]]

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

object HeaderRepo extends ContextEmbed[HeaderRepo] {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[HeaderRepo[D]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler)
    extends HeaderRepo[D] {

    import org.ergoplatform.explorer.db.queries.{HeaderQuerySet => QS}

    def insert(h: Header): D[Unit] =
      QS.insert(h).void.liftConnIO

    def get(id: Id): D[Option[Header]] =
      QS.get(id).option.liftConnIO

    def getLast: D[Option[Header]] =
      QS.getLast.option.liftConnIO

    def getByParentId(parentId: Id): D[Option[Header]] =
      QS.getByParentId(parentId).option.liftConnIO

    def getAllByHeight(height: Int): D[List[Header]] =
      QS.getAllByHeight(height).to[List].liftConnIO

    def getHeightOf(id: Id): D[Option[Int]] =
      QS.getHeightOf(id).option.liftConnIO

    def getBestHeight: D[Int] =
      QS.getBestHeight.option
        .map(_.getOrElse(constants.PreGenesisHeight))
        .liftConnIO

    def updateChainStatusById(id: Id, newChainStatus: Boolean): D[Unit] =
      QS.updateChainStatusById(id, newChainStatus).run.void.liftConnIO
  }
}
