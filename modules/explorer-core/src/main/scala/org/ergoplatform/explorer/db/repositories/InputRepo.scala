package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import doobie.free.implicits._
import doobie.refined.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.models.Input
import org.ergoplatform.explorer.db.models.aggregates.ExtendedInput
import org.ergoplatform.explorer.db.doobieInstances._
import org.ergoplatform.explorer.db.repositories.HeaderRepo.Live

/** [[Input]] and [[ExtendedInput]] data access operations.
  */
trait InputRepo[D[_]] {

  /** Put a given `input` to persistence.
    */
  def insert(input: Input): D[Unit]

  /** Put a given list of inputs to persistence.
    */
  def insetMany(inputs: List[Input]): D[Unit]

  /** Get all inputs related to a given `txId`.
    */
  def getAllByTxId(txId: TxId): D[List[ExtendedInput]]

  /** Get all inputs related to a given list of `txId`.
    */
  def getAllByTxIds(txsId: NonEmptyList[TxId]): D[List[ExtendedInput]]
}

object InputRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[InputRepo[D]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler)
    extends InputRepo[D] {

    import org.ergoplatform.explorer.db.queries.{InputQuerySet => QS}

    def insert(input: Input): D[Unit] =
      QS.insert(input).void.liftConnectionIO

    def insetMany(inputs: List[Input]): D[Unit] =
      QS.insertMany(inputs).void.liftConnectionIO

    def getAllByTxId(txId: TxId): D[List[ExtendedInput]] =
      QS.getAllByTxId(txId).to[List].liftConnectionIO

    def getAllByTxIds(txIds: NonEmptyList[TxId]): D[List[ExtendedInput]] =
      QS.getAllByTxIds(txIds).to[List].liftConnectionIO
  }
}
