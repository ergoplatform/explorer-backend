package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import doobie.free.implicits._
import doobie.util.log.LogHandler
import fs2.Stream
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.UTransaction
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.{ErgoTree, HexString, TxId}
import org.ergoplatform.explorer.constraints.OrderingString

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
  ): D[List[UTransaction]]

  def streamRelatedToErgoTree(
    ergoTree: ErgoTree,
    offset: Int,
    limit: Int
  ): S[D, UTransaction]

  /** Get all unconfirmed transactions.
    */
  def getAll(offset: Int, limit: Int): D[List[UTransaction]]

  /** Get all unconfirmed transactions with sorting
    */
  def getAll(offset: Int, limit: Int, order: OrderingString, sortBy: String): D[List[UTransaction]]

  /** Get ids of all unconfirmed transactions.
    */
  def getAllIds: D[List[TxId]]

  /** Get total number of unconfirmed transactions.
    */
  def countAll: D[Int]

  /** Count unconfirmed transactions related to the given `ergoTree`.
    */
  def countByErgoTree(ergoTree: HexString): D[Int]
}

object UTransactionRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[UTransactionRepo[D, Stream]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler) extends UTransactionRepo[D, Stream] {

    import org.ergoplatform.explorer.db.queries.{UTransactionQuerySet => QS}

    val liftK = LiftConnectionIO[D].liftConnectionIOK

    def insert(tx: UTransaction): D[Unit] =
      QS.insertNoConflict(tx).void.liftConnectionIO

    def insertMany(txs: List[UTransaction]): D[Unit] =
      QS.insertManyNoConflict(txs).void.liftConnectionIO

    def dropMany(ids: NonEmptyList[TxId]): D[Int] =
      QS.dropMany(ids).run.liftConnectionIO

    def get(id: TxId): D[Option[UTransaction]] =
      QS.get(id).option.liftConnectionIO

    def getAllRelatedToErgoTree(
      ergoTree: HexString,
      offset: Int,
      limit: Int
    ): D[List[UTransaction]] =
      QS.getAllRelatedToErgoTree(ergoTree, offset, limit)
        .to[List]
        .liftConnectionIO

    def streamRelatedToErgoTree(ergoTree: ErgoTree, offset: Int, limit: Int): Stream[D, UTransaction] =
      QS.getAllRelatedToErgoTree(ergoTree.value, offset, limit).stream.translate(liftK)

    def getAll(offset: Int, limit: Int): D[List[UTransaction]] =
      QS.getAll(offset, limit).to[List].liftConnectionIO

    def getAll(offset: Int, limit: Int, order: OrderingString, sortBy: String): D[List[UTransaction]] =
      QS.getAll(offset, limit).to[List].liftConnectionIO

    def getAllIds: D[List[TxId]] =
      QS.getAllIds.to[List].liftConnectionIO

    def countAll: D[Int] =
      QS.countUnconfirmedTxs.unique.liftConnectionIO

    def countByErgoTree(ergoTree: HexString): D[Int] =
      QS.countByErgoTree(ergoTree).unique.liftConnectionIO
  }
}
