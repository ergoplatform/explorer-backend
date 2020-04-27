package org.ergoplatform.explorer.db.repositories

import cats.effect.Sync
import cats.implicits._
import fs2.Stream
import doobie.free.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.syntax.liftConnIO._
import org.ergoplatform.explorer.db.models.Transaction
import org.ergoplatform.explorer.{Address, Id, LiftConnectionIO, TxId}

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
  ): D[List[Transaction]]

  /** Get total number of transactions related to a given `address`.
    */
  def countRelatedToAddress(address: Address): D[Int]

  /** Get total number of transactions appeared in the main chain after a given timestamp `ts`.
    */
  def countMainSince(ts: Long): D[Int]

  /** Get transactions appeared in the main-chain after given height.
    */
  def getMainSince(
    height: Int,
    offset: Int,
    limit: Int
  ): D[List[Transaction]]

  /** Get all ids matching the given `query`.
    */
  def getIdsLike(query: String): D[List[TxId]]

  /** Update main_chain flag with a given `newChainStatus` for all txs related to given `headerId`.
    */
  def updateChainStatusByHeaderId(headerId: Id, newChainStatus: Boolean): D[Unit]
}

object TransactionRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[TransactionRepo[D, Stream]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler)
    extends TransactionRepo[D, Stream] {

    import org.ergoplatform.explorer.db.queries.{TransactionQuerySet => QS}

    private val liftK = implicitly[LiftConnectionIO[D]].liftF

    def insert(tx: Transaction): D[Unit] =
      QS.insert(tx).void.liftConnIO

    def insertMany(txs: List[Transaction]): D[Unit] =
      QS.insertMany(txs).void.liftConnIO

    def getMain(id: TxId): D[Option[Transaction]] =
      QS.getMain(id).option.liftConnIO

    def getAllMainByIdSubstring(idStr: String): D[List[Transaction]] =
      QS.getAllMainByIdSubstring(idStr).to[List].liftConnIO

    def getAllByBlockId(id: Id): Stream[D, Transaction] =
      QS.getAllByBlockId(id).stream.translate(liftK)

    def getRelatedToAddress(
      address: Address,
      offset: Int,
      limit: Int
    ): D[List[Transaction]] =
      QS.getAllRelatedToAddress(address, offset, limit).to[List].liftConnIO

    def countRelatedToAddress(address: Address): D[Int] =
      QS.countRelatedToAddress(address).unique.liftConnIO

    def countMainSince(ts: Long): D[Int] =
      QS.countMainSince(ts).unique.liftConnIO

    def getMainSince(
      height: Int,
      offset: Int,
      limit: Int
    ): D[List[Transaction]] =
      QS.getAllMainSince(height, offset, limit).to[List].liftConnIO

    def getIdsLike(query: String): D[List[TxId]] =
      QS.getIdsLike(query).to[List].liftConnIO

    def updateChainStatusByHeaderId(headerId: Id, newChainStatus: Boolean): D[Unit] =
      QS.updateChainStatusByHeaderId(headerId, newChainStatus).run.void.liftConnIO
  }
}
