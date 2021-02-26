package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.functor._
import doobie.free.implicits._
import doobie.refined.implicits._
import doobie.util.log.LogHandler
import fs2.Stream
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.UInput
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.doobieInstances._
import org.ergoplatform.explorer.db.models.aggregates.ExtendedUInput

/** [[UInput]] data access operations.
  */
trait UInputRepo[D[_], S[_[_], _]] {

  /** Put a given `input` to persistence.
    */
  def insert(input: UInput): D[Unit]

  /** Put a given list of inputs to persistence.
    */
  def insetMany(inputs: List[UInput]): D[Unit]

  /** Get all inputs containing in unconfirmed transactions.
    */
  def getAll(offset: Int, limit: Int): S[D, ExtendedUInput]

  /** Get all inputs related to transaction with a given `txId`.
    */
  def getAllByTxId(txId: TxId): D[List[ExtendedUInput]]

  /** Get all inputs related to transaction with a given list of `txId`.
    */
  def getAllByTxIds(txIds: NonEmptyList[TxId]): D[List[ExtendedUInput]]
}

object UInputRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[UInputRepo[D, Stream]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler)
    extends UInputRepo[D, Stream] {

    import org.ergoplatform.explorer.db.queries.{UInputQuerySet => QS}

    def insert(input: UInput): D[Unit] =
      QS.insertNoConflict(input).void.liftConnectionIO

    def insetMany(inputs: List[UInput]): D[Unit] =
      QS.insertManyNoConflict(inputs).void.liftConnectionIO

    def getAll(offset: Int, limit: Int): Stream[D, ExtendedUInput] =
      QS.getAll(offset, limit).stream.translate(LiftConnectionIO[D].liftConnectionIOK)

    def getAllByTxId(txId: TxId): D[List[ExtendedUInput]] =
      QS.getAllByTxId(txId).to[List].liftConnectionIO

    def getAllByTxIds(txIds: NonEmptyList[TxId]): D[List[ExtendedUInput]] =
      QS.getAllByTxIds(txIds).to[List].liftConnectionIO
  }
}
