package org.ergoplatform.explorer.db.repositories

import cats.implicits._
import fs2.Stream
import doobie.free.implicits._
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.models.Transaction
import org.ergoplatform.explorer.{Address, Id, TxId}

/** [[Transaction]] data access operations.
  */
trait TransactionRepo[D[_], S[_[_], _]] {

  /** Put a given `tx` to persistence.
    */
  def insert(tx: Transaction): D[Unit]

  /** Put a given list of transactions to persistence.
    */
  def insertMany(txs: List[Transaction]): D[Unit]

  /** Get transaction with a given `id` from main-chain.
    */
  def getMain(id: TxId): D[Option[Transaction]]

  /** Get all transactions with id matching a given `idStr` from main-chain.
    */
  def getAllMainByIdSubstring(substring: String): D[List[Transaction]]

  /** Get all transactions from block with a given `id`.
    */
  def getAllByBlockId(id: Id): S[D, Transaction]

  /** Get transactions related to a given `address`.
    */
  def getRelatedToAddress(
    address: Address,
    offset: Int,
    limit: Int
  ): S[D, Transaction]

  /** Get transactions appeared in the main-chain after given height.
    */
  def getMainSince(
    height: Int,
    offset: Int,
    limit: Int
  ): S[D, Transaction]
}

object TransactionRepo {

  def apply[D[_]: LiftConnectionIO]: TransactionRepo[D, Stream] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO] extends TransactionRepo[D, Stream] {

    import org.ergoplatform.explorer.db.queries.{TransactionQuerySet => QS}

    private val liftK = LiftConnectionIO[D].liftConnectionIOK

    def insert(tx: Transaction): D[Unit] =
      QS.insert(tx).void.liftConnectionIO

    def insertMany(txs: List[Transaction]): D[Unit] =
      QS.insertMany(txs).void.liftConnectionIO

    def getMain(id: TxId): D[Option[Transaction]] =
      QS.getMain(id).option.liftConnectionIO

    def getAllMainByIdSubstring(idStr: String): D[List[Transaction]] =
      QS.getAllMainByIdSubstring(idStr).to[List].liftConnectionIO

    def getAllByBlockId(id: Id): Stream[D, Transaction] =
      QS.getAllByBlockId(id).stream.translate(liftK)

    def getRelatedToAddress(
      address: Address,
      offset: Int,
      limit: Int
    ): Stream[D, Transaction] =
      QS.getAllRelatedToAddress(address, offset, limit).stream.translate(liftK)

    def getMainSince(
      height: Int,
      offset: Int,
      limit: Int
    ): Stream[D, Transaction] =
      QS.getAllMainSince(height, offset, limit).stream.translate(liftK)
  }
}
