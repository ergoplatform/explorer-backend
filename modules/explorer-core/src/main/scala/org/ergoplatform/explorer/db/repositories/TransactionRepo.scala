package org.ergoplatform.explorer.db.repositories

import cats.effect.Sync
import cats.implicits._
import doobie.free.implicits._
import doobie.util.log.LogHandler
import fs2.Stream
import org.ergoplatform.explorer.constraints.OrderingString
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.Transaction
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.{Address, BlockId, ErgoTreeTemplateHash, TxId}

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
  def getAllByBlockId(id: BlockId): S[D, Transaction]

  /** Get transaction ids from latest block from main-chain.
    */
  def getRecentIds: D[List[TxId]]

  /** Get transactions related to a given `address`.
    */
  def getRelatedToAddress(
    address: Address,
    offset: Int,
    limit: Int
  ): D[List[Transaction]]

  /** Get transactions related to a given `address`.
    */
  def streamRelatedToAddress(
    address: Address,
    offset: Int,
    limit: Int
  ): S[D, Transaction]

  def streamTransactions(minGix: Long, limit: Int): S[D, Transaction]

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

  /** Get transactions whose inputs contains a given script template hash.
    */
  def getByInputsScriptTemplate(
    template: ErgoTreeTemplateHash,
    offset: Int,
    limit: Int,
    ordering: OrderingString
  ): S[D, Transaction]

  /** Count transactions whose inputs contains a given script template hash.
    */
  def countByInputsScriptTemplate(template: ErgoTreeTemplateHash): D[Int]

  /** Update main_chain flag with a given `newChainStatus` for all txs related to given `headerId`.
    */
  def updateChainStatusByHeaderId(headerId: BlockId, newChainStatus: Boolean): D[Unit]
}

object TransactionRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[TransactionRepo[D, Stream]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler) extends TransactionRepo[D, Stream] {

    import org.ergoplatform.explorer.db.queries.{TransactionQuerySet => QS}

    private val liftK = LiftConnectionIO[D].liftConnectionIOK

    def insert(tx: Transaction): D[Unit] =
      QS.insertNoConflict(tx).void.liftConnectionIO

    def insertMany(txs: List[Transaction]): D[Unit] =
      QS.insertManyNoConflict(txs).void.liftConnectionIO

    def getMain(id: TxId): D[Option[Transaction]] =
      QS.getMain(id).option.liftConnectionIO

    def getAllMainByIdSubstring(idStr: String): D[List[Transaction]] =
      QS.getAllMainByIdSubstring(idStr).to[List].liftConnectionIO

    def getAllByBlockId(id: BlockId): Stream[D, Transaction] =
      QS.getAllByBlockId(id).stream.translate(liftK)

    def getRecentIds: D[List[TxId]] =
      QS.getRecentIds.to[List].liftConnectionIO

    def getRelatedToAddress(
      address: Address,
      offset: Int,
      limit: Int
    ): D[List[Transaction]] =
      QS.getAllRelatedToAddress(address, offset, limit).to[List].liftConnectionIO

    def streamRelatedToAddress(
      address: Address,
      offset: Int,
      limit: Int
    ): Stream[D, Transaction] =
      QS.getAllRelatedToAddress(address, offset, limit).stream.translate(liftK)

    def streamTransactions(minGix: Long, limit: Int): Stream[D, Transaction] =
      QS.getAll(minGix, limit).stream.translate(liftK)

    def countRelatedToAddress(address: Address): D[Int] =
      QS.countRelatedToAddress(address).unique.liftConnectionIO

    def countMainSince(ts: Long): D[Int] =
      QS.countMainSince(ts).unique.liftConnectionIO

    def getMainSince(
      height: Int,
      offset: Int,
      limit: Int
    ): D[List[Transaction]] =
      QS.getAllMainSince(height, offset, limit).to[List].liftConnectionIO

    def getIdsLike(query: String): D[List[TxId]] =
      QS.getIdsLike(query).to[List].liftConnectionIO

    def getByInputsScriptTemplate(
      template: ErgoTreeTemplateHash,
      offset: Int,
      limit: Int,
      ordering: OrderingString
    ): Stream[D, Transaction] =
      QS.getByInputsScriptTemplate(template, offset, limit, ordering).stream.translate(liftK)

    def countByInputsScriptTemplate(template: ErgoTreeTemplateHash): D[Int] =
      QS.countByInputsScriptTemplate(template).unique.liftConnectionIO

    def updateChainStatusByHeaderId(headerId: BlockId, newChainStatus: Boolean): D[Unit] =
      QS.updateChainStatusByHeaderId(headerId, newChainStatus).run.void.liftConnectionIO
  }
}
