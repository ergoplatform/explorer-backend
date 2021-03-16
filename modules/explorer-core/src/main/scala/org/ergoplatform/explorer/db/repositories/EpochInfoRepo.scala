package org.ergoplatform.explorer.db.repositories

import cats.effect.Sync
import cats.syntax.functor._
import doobie.free.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.ErgoLikeContext.Height
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.EpochParameters
import org.ergoplatform.explorer.protocol.constants

/** [[EpochParameters]] data access operations.
  */
trait EpochInfoRepo[D[_]] {

  /** Put a given `parameters` to persistence.
    */
  def insert(parameters: EpochParameters): D[Unit]

  /** Get epoch parameters with a given id
    */
  def getByEpochId(id: Int): D[Option[EpochParameters]]

  /** Get height of the last known header in epoch.
    */
  def getLastHeight: D[Int]

  /** Get info about last epoch
    */
  def getLastEpoch: D[Option[EpochParameters]]
}

object EpochInfoRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[EpochInfoRepo[D]] =
    DoobieLogHandler.create[F].map(implicit lh =>
      new Live[D]
    )

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler) extends EpochInfoRepo[D] {

    import org.ergoplatform.explorer.db.queries.{EpochParametersQuerySet => QS}

    override def insert(parameters: EpochParameters): D[Unit] =
      QS.insertNoConflict(parameters).void.liftConnectionIO

    override def getByEpochId(id: Int): D[Option[EpochParameters]] =
      QS.getById(id).option.liftConnectionIO

    override def getLastHeight: D[Int] =
      QS.getLastHeight.option
        .map(_.getOrElse(constants.PreGenesisHeight))
        .liftConnectionIO

    override def getLastEpoch: D[Option[EpochParameters]] =
      QS.getLastEpoch.option.liftConnectionIO
  }
}
