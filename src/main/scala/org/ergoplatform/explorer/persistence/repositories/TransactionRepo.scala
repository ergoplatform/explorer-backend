package org.ergoplatform.explorer.persistence.repositories

import cats.effect.Sync
import cats.implicits._
import doobie.util.transactor.Transactor
import doobie.implicits._
import fs2.Stream
import org.ergoplatform.explorer.persistence.models.Transaction
import org.ergoplatform.explorer.{Address, Id, TxId}

/** [[Transaction]] data access operations.
  */
trait TransactionRepo[F[_]] {

  /** Put a given `tx` to persistence.
    */
  def insert(tx: Transaction): F[Unit]

  /** Get transaction with a given `id` from main-chain.
    */
  def getMain(id: TxId): F[Option[Transaction]]

  /** Get all transactions with id matching a given `idStr` from main-chain.
    */
  def getAllMainByIdSubstring(idStr: String): F[List[Transaction]]

  /** Get all transactions from block with a given `id`.
    */
  def getAllByBlockId(id: Id): Stream[F, Transaction]

  /** Get all transaction related to a given `address`.
    */
  def getAllRelatedToAddress(
    address: Address,
    offset: Int,
    limit: Int
  ): Stream[F, Transaction]

  /** Get all transactions appeared in the main-chain after given height.
    */
  def getAllMainSince(
    height: Int,
    offset: Int,
    limit: Int
  ): Stream[F, Transaction]
}

object TransactionRepo {

  final class Live[F[_]: Sync](xa: Transactor[F])
    extends TransactionRepo[F] {

    import org.ergoplatform.explorer.persistence.dao.{TransactionDao => dao}

    def insert(tx: Transaction): F[Unit] =
      dao.insert(tx).transact(xa).as(())

    def getMain(id: TxId): F[Option[Transaction]] =
      dao.getMain(id).transact(xa)

    def getAllMainByIdSubstring(idStr: String): F[List[Transaction]] =
      dao.getAllMainByIdSubstring(idStr).transact(xa)

    def getAllByBlockId(id: Id): Stream[F, Transaction] =
      dao.getAllByBlockId(id).transact(xa)

    def getAllRelatedToAddress(
      address: Address,
      offset: Int,
      limit: Int
    ): Stream[F, Transaction] = ???

    def getAllMainSince(
      height: Int,
      offset: Int,
      limit: Int
    ): Stream[F, Transaction] = ???
  }
}
