package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import doobie.free.implicits._
import doobie.util.log.LogHandler
import fs2.Stream
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.models.UTransaction
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.{HexString, LiftConnectionIO, TxId}

trait UTransactionRepo[D[_], S[_[_], _]] {

  /** Put a given `tx` to persistence.
    */
  def insert(tx: UTransaction): D[Unit]

  /** Put a given list of transactions to persistence.
    */
  def insertMany(txs: List[UTransaction]): D[Unit]

  /** Drop all transactions with the given list of `ids`.
    */
  def dropMany(ids: NonEmptyList[TxId]): D[Int]

  /** Get unconfirmed transaction with a given `id`.
    */
  def get(id: TxId): D[Option[UTransaction]]

  /** Get all unconfirmed transactions related to the given `ergoTree`.
    */
  def getAllRelatedToErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int
  ): S[D, UTransaction]

  /** Get all unconfirmed transactions.
    */
  def getAll(offset: Int, limit: Int): D[List[UTransaction]]

  /** Get ids of all unconfirmed transactions.
    */
  def getAllIds: D[List[TxId]]

  /** Get total number of unconfirmed transactions.
    */
  def countAll: D[Int]
}

object UTransactionRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[UTransactionRepo[D, Stream]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler)
    extends UTransactionRepo[D, Stream] {

    import org.ergoplatform.explorer.db.queries.{UTransactionQuerySet => QS}

    def insert(tx: UTransaction): D[Unit] =
      QS.insert(tx).void.liftConnIO

    def insertMany(txs: List[UTransaction]): D[Unit] =
      QS.insertMany(txs).void.liftConnIO

    def dropMany(ids: NonEmptyList[TxId]): D[Int] =
      QS.dropMany(ids).run.liftConnIO

    def get(id: TxId): D[Option[UTransaction]] =
      QS.get(id).option.liftConnIO

    def getAllRelatedToErgoTree(
      ergoTree: HexString,
      offset: Int,
      limit: Int
    ): Stream[D, UTransaction] =
      QS.getAllRelatedToErgoTree(ergoTree, offset, limit)
        .stream
        .translate(implicitly[LiftConnectionIO[D]].liftF)

    def getAll(offset: Int, limit: Int): D[List[UTransaction]] =
      QS.getAll(offset, limit).to[List].liftConnIO

    def getAllIds: D[List[TxId]] =
      QS.getAllIds.to[List].liftConnIO

    def countAll: D[Int] =
      QS.countUnconfirmedTxs.unique.liftConnIO
  }
}
