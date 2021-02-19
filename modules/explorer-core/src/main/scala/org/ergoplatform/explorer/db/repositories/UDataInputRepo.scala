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
import org.ergoplatform.explorer.db.models.UDataInput
import org.ergoplatform.explorer.db.models.aggregates.ExtendedUDataInput
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._

/** [[UDataInput]] data access operations.
  */
trait UDataInputRepo[D[_], S[_[_], _]] {

  /** Put a given `input` to persistence.
    */
  def insert(input: UDataInput): D[Unit]

  /** Put a given list of inputs to persistence.
    */
  def insetMany(inputs: List[UDataInput]): D[Unit]

  /** Get all inputs containing in unconfirmed transactions.
    */
  def getAll(offset: Int, limit: Int): S[D, ExtendedUDataInput]

  /** Get all inputs related to transaction with a given `txId`.
    */
  def getAllByTxId(txId: TxId): D[List[ExtendedUDataInput]]

  /** Get all inputs related to transaction with a given list of `txId`.
    */
  def getAllByTxIds(txIds: NonEmptyList[TxId]): D[List[ExtendedUDataInput]]
}

object UDataInputRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[UDataInputRepo[D, Stream]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler)
    extends UDataInputRepo[D, Stream] {

    import org.ergoplatform.explorer.db.queries.{UDataInputQuerySet => QS}

    def insert(input: UDataInput): D[Unit] =
      QS.insertNoConflict(input).void.liftConnectionIO

    def insetMany(inputs: List[UDataInput]): D[Unit] =
      QS.insertManyNoConflict(inputs).void.liftConnectionIO

    def getAll(offset: Int, limit: Int): Stream[D, ExtendedUDataInput] =
      QS.getAll(offset, limit).stream.translate(LiftConnectionIO[D].liftConnectionIOK)

    def getAllByTxId(txId: TxId): D[List[ExtendedUDataInput]] =
      QS.getAllByTxId(txId).to[List].liftConnectionIO

    def getAllByTxIds(txIds: NonEmptyList[TxId]): D[List[ExtendedUDataInput]] =
      QS.getAllByTxIxs(txIds).to[List].liftConnectionIO
  }
}
