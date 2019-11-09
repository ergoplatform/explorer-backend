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
trait TransactionRepo[F[_], G[_]] {

  /** Put a given `tx` to persistence.
    */
  def insert(tx: Transaction): F[Unit]

  /** Get transaction with a given `id` from main-chain.
    */
  def getMain(id: TxId): F[Option[Transaction]]

  /** Get all transactions with id matching a given `idStr` from main-chain.
    */
  def getAllMainByIdSubstring(substring: String): F[List[Transaction]]

  /** Get all transactions from block with a given `id`.
    */
  def getAllByBlockId(id: Id): G[Transaction]

  /** Get all transaction related to a given `address`.
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

  final class Live[F[_]: Sync](xa: Transactor[F])
    extends TransactionRepo[F, Stream[F, *]] {

    import org.ergoplatform.explorer.persistence.queries.{TransactionQuerySet => QS}

    def insert(tx: Transaction): F[Unit] =
      QS.insert(tx).transact(xa).as(())

    def getMain(id: TxId): F[Option[Transaction]] =
      QS.getMain(id).transact(xa)

    def getAllMainByIdSubstring(idStr: String): F[List[Transaction]] =
      QS.getAllMainByIdSubstring(idStr).transact(xa)

    def getAllByBlockId(id: Id): Stream[F, Transaction] =
      QS.getAllByBlockId(id).transact(xa)

    def getAllRelatedToAddress(
      address: Address,
      offset: Int,
      limit: Int
    ): Stream[F, Transaction] =
      QS.getAllRelatedToAddress(address, offset, limit).transact(xa)

    def getAllMainSince(
      height: Int,
      offset: Int,
      limit: Int
    ): Stream[F, Transaction] =
      QS.getAllMainSince(height, offset, limit).transact(xa)
  }
}
