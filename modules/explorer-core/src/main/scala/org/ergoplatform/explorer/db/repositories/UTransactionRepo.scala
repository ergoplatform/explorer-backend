package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.implicits._
import doobie.free.implicits._
import fs2.Stream
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.UTransaction
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.{HexString, TxId}

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

  /** Get ids of all unconfirmed transactions.
    */
  def getAllIds: D[List[TxId]]
}

object UTransactionRepo {

  def apply[D[_]: LiftConnectionIO]: UTransactionRepo[D, Stream] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO] extends UTransactionRepo[D, Stream] {

    import org.ergoplatform.explorer.db.queries.{UTransactionQuerySet => QS}

    def insert(tx: UTransaction): D[Unit] =
      QS.insert(tx).void.liftConnectionIO

    def insertMany(txs: List[UTransaction]): D[Unit] =
      QS.insertMany(txs).void.liftConnectionIO

    def dropMany(ids: NonEmptyList[TxId]): D[Int] =
      QS.dropMany(ids).run.liftConnectionIO

    def get(id: TxId): D[Option[UTransaction]] =
      QS.get(id).option.liftConnectionIO

    def getAllRelatedToErgoTree(
      ergoTree: HexString,
      offset: Int,
      limit: Int
    ): Stream[D, UTransaction] =
      QS.getAllRelatedToErgoTree(ergoTree, offset, limit)
        .stream
        .translate(LiftConnectionIO[D].liftConnectionIOK)

    def getAllIds: D[List[TxId]] = ???
  }
}
