package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.effect.Sync
import fs2.Stream
import cats.implicits._
import doobie.free.implicits._
import doobie.refined.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.{HexString, TxId}
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.models.UOutput
import org.ergoplatform.explorer.db.doobieInstances._

/** [[UOutput]] data access operations.
  */
trait UOutputRepo[D[_], S[_[_], _]] {

  /** Put a given unconfirmed `output` to persistence.
    */
  def insert(output: UOutput): D[Unit]

  /** Put a given list of unconfirmed outputs to persistence.
    */
  def insertMany(outputs: List[UOutput]): D[Unit]

  /** Get all outputs containing in unconfirmed transactions.
    */
  def getAll(offset: Int, limit: Int): S[D, UOutput]

  /** Get all unconfirmed outputs related to transaction with a given `txId`.
    */
  def getAllByTxId(txId: TxId): D[List[UOutput]]

  /** Get all unconfirmed outputs related to transaction with a given list of `txId`.
    */
  def getAllByTxIds(txIds: NonEmptyList[TxId]): D[List[UOutput]]

  /** Get all unconfirmed outputs belonging to the given `ergoTree`.
    */
  def getAllByErgoTree(ergoTree: HexString): D[List[UOutput]]

  /** Get all unspent unconfirmed outputs belonging to the given `ergoTree`.
    */
  def getAllUnspentByErgoTree(ergoTree: HexString): D[List[UOutput]]
}

object UOutputRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[UOutputRepo[D, Stream]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler)
    extends UOutputRepo[D, Stream] {

    import org.ergoplatform.explorer.db.queries.{UOutputQuerySet => QS}

    def insert(output: UOutput): D[Unit] =
      QS.insert(output).void.liftConnectionIO

    def insertMany(outputs: List[UOutput]): D[Unit] =
      QS.insertMany(outputs).void.liftConnectionIO

    def getAll(offset: Int, limit: Int): Stream[D, UOutput] =
      QS.getAll(offset, limit).stream.translate(LiftConnectionIO[D].liftConnectionIOK)

    def getAllByTxId(txId: TxId): D[List[UOutput]] =
      QS.getAllByTxId(txId).to[List].liftConnectionIO

    def getAllByTxIds(txIds: NonEmptyList[TxId]): D[List[UOutput]] =
      QS.getAllByTxIds(txIds).to[List].liftConnectionIO

    def getAllByErgoTree(ergoTree: HexString): D[List[UOutput]] =
      QS.getAllByErgoTree(ergoTree).to[List].liftConnectionIO

    def getAllUnspentByErgoTree(ergoTree: HexString): D[List[UOutput]] =
      QS.getAllUnspentByErgoTree(ergoTree).to[List].liftConnectionIO
  }
}
