package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.functor._
import doobie.free.implicits._
import doobie.refined.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.doobieInstances._
import org.ergoplatform.explorer.db.models.DataInput
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedDataInput, FullDataInput, FullInput}
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.{BlockId, TxId}

/** [[DataInput]] and [[ExtendedDataInput]] data access operations.
  */
trait DataInputRepo[D[_]] {

  /** Put a given `input` to persistence.
    */
  def insert(input: DataInput): D[Unit]

  /** Put a given list of inputs to persistence.
    */
  def insetMany(inputs: List[DataInput]): D[Unit]

  /** Get all inputs related to a given `txId`.
    */
  def getAllByTxId(txId: TxId): D[List[ExtendedDataInput]]

  /** Get all inputs related to a given list of `txId`.
    */
  def getAllByTxIds(txsId: NonEmptyList[TxId]): D[List[ExtendedDataInput]]

  /** Get all inputs related to a given `txId`.
    */
  def getFullByTxId(txId: TxId): D[List[FullDataInput]]

  /** Get all inputs related to a given list of `txId`.
    */
  def getFullByTxIds(txIds: NonEmptyList[TxId]): D[List[FullDataInput]]

  /** Update main_chain status for all inputs related to given `headerId`.
    */
  def updateChainStatusByHeaderId(headerId: BlockId, newChainStatus: Boolean): D[Unit]
}

object DataInputRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[DataInputRepo[D]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler) extends DataInputRepo[D] {

    import org.ergoplatform.explorer.db.queries.{DataInputQuerySet => QS}

    def insert(input: DataInput): D[Unit] =
      QS.insertNoConflict(input).void.liftConnectionIO

    def insetMany(inputs: List[DataInput]): D[Unit] =
      QS.insertManyNoConflict(inputs).void.liftConnectionIO

    def getAllByTxId(txId: TxId): D[List[ExtendedDataInput]] =
      QS.getAllByTxId(txId).to[List].liftConnectionIO

    def getAllByTxIds(txIds: NonEmptyList[TxId]): D[List[ExtendedDataInput]] =
      QS.getAllByTxIds(txIds).to[List].liftConnectionIO

    def getFullByTxId(txId: TxId): D[List[FullDataInput]] =
      QS.getFullByTxId(txId).to[List].liftConnectionIO

    def getFullByTxIds(txIds: NonEmptyList[TxId]): D[List[FullDataInput]] =
      QS.getFullByTxIds(txIds).to[List].liftConnectionIO

    def updateChainStatusByHeaderId(headerId: BlockId, newChainStatus: Boolean): D[Unit] =
      QS.updateChainStatusByHeaderId(headerId, newChainStatus).run.void.liftConnectionIO
  }
}
