package org.ergoplatform.explorer.db.repositories

import cats.implicits._
import fs2.Stream
import doobie.free.implicits._
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.algebra.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.models.Transaction
import org.ergoplatform.explorer.{Address, Id, TxId}

/** [[Transaction]] data access operations.
  */
trait TransactionRepo[D[_], G[_]] {

  /** Put a given `tx` to persistence.
    */
  def insert(tx: Transaction): D[Unit]

  /** Get transaction with a given `id` from main-chain.
    */
  def getMain(id: TxId): D[Option[Transaction]]

  /** Get all transactions with id matching a given `idStr` from main-chain.
    */
  def getAllMainByIdSubstring(substring: String): D[List[Transaction]]

  /** Get all transactions from block with a given `id`.
    */
  def getAllByBlockId(id: Id): G[Transaction]

  /** Get all transactions related to a given `address`.
    */
  def getAllRelatedToAddress(
    address: Address,
    offset: Int,
    limit: Int
  ): G[Transaction]

  /** Get all transactions appeared in the main-chain after given height.
    */
  def getAllMainSince(
    height: Int,
    offset: Int,
    limit: Int
  ): G[Transaction]
}

object TransactionRepo {

  def apply[D[_]: LiftConnectionIO]: TransactionRepo[D, Stream[D, *]] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO]
    extends TransactionRepo[D, Stream[D, *]] {

    import org.ergoplatform.explorer.db.queries.{TransactionQuerySet => QS}

    private val liftK = LiftConnectionIO[D].liftConnectionIOK

    def insert(tx: Transaction): D[Unit] =
      QS.insert(tx).void.liftConnectionIO

    def getMain(id: TxId): D[Option[Transaction]] =
      QS.getMain(id).liftConnectionIO

    def getAllMainByIdSubstring(idStr: String): D[List[Transaction]] =
      QS.getAllMainByIdSubstring(idStr).liftConnectionIO

    def getAllByBlockId(id: Id): Stream[D, Transaction] =
      QS.getAllByBlockId(id).translate(liftK)

    def getAllRelatedToAddress(
      address: Address,
      offset: Int,
      limit: Int
    ): Stream[D, Transaction] =
      QS.getAllRelatedToAddress(address, offset, limit).translate(liftK)

    def getAllMainSince(
      height: Int,
      offset: Int,
      limit: Int
    ): Stream[D, Transaction] =
      QS.getAllMainSince(height, offset, limit).translate(liftK)
  }
}
