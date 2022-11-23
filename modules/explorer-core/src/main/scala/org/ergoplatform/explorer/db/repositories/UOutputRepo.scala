package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.effect.Sync
import fs2.Stream
import cats.implicits._
import doobie.free.implicits._
import doobie.refined.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.explorer.constraints.OrderingString
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.{BoxId, ErgoTree, HexString, TxId}
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.doobieInstances._
import org.ergoplatform.explorer.db.models.{AnyOutput, UOutput}
import org.ergoplatform.explorer.db.models.aggregates.ExtendedUOutput

/** [[ExtendedUOutput]] data access operations.
  */
trait UOutputRepo[D[_], S[_[_], _]] {

  /** Put a given unconfirmed `output` to persistence.
    */
  def insert(output: UOutput): D[Unit]

  /** Put a given list of unconfirmed outputs to persistence.
    */
  def insertMany(outputs: List[UOutput]): D[Unit]

  /** Get a box by `boxId`.
    */
  def getByBoxId(boxId: BoxId): D[Option[ExtendedUOutput]]

  /** Get all outputs containing in unconfirmed transactions.
    */
  def streamAll(offset: Int, limit: Int): S[D, ExtendedUOutput]

  /** Get all unspent outputs containing in unconfirmed transactions.
    */
  def streamAllUnspent(offset: Int, limit: Int): S[D, UOutput]

  /** Get all unconfirmed outputs related to transaction with a given `txId`.
    */
  def getAllByTxId(txId: TxId): D[List[ExtendedUOutput]]

  /** Get all unconfirmed outputs related to transaction with a given list of `txId`.
    */
  def getAllByTxIds(txIds: NonEmptyList[TxId]): D[List[ExtendedUOutput]]

  /** Get all unconfirmed outputs related to an address.
    */
  def streamAllRelatedToErgoTree(ergoTree: ErgoTree): Stream[D, UOutput]

  /** Get all unconfirmed outputs belonging to the given `ergoTree`.
    */
  def getAllByErgoTree(ergoTree: HexString): D[List[ExtendedUOutput]]

  /** Get all unspent unconfirmed outputs belonging to the given `ergoTree`.
    */
  def getAllUnspentByErgoTree(ergoTree: HexString): D[List[ExtendedUOutput]]

  /** Get confirmed + unconfirmed unspent main-chain outputs with a given `ergoTree`.
    */
  def streamAllUnspentByErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int,
    ordering: OrderingString
  ): S[D, AnyOutput]

  /** Get total amount of all unspent main-chain outputs with a given `ergoTree`.
    */
  def sumUnspentByErgoTree(ergoTree: HexString): D[Long]

  /** Count unspent main-chain outputs with a given `ergoTree`.
    */
  def countUnspentByErgoTree(ergoTree: HexString): D[Int]

  /** Count confirmed + unconfirmed unspent main-chain outputs with a given `ergoTree`.
    */
  def countAllByErgoTree(ergoTree: HexString): D[Int]
}

object UOutputRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[UOutputRepo[D, Stream]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler) extends UOutputRepo[D, Stream] {

    import org.ergoplatform.explorer.db.queries.{UOutputQuerySet => QS}

    def insert(output: UOutput): D[Unit] =
      QS.insertNoConflict(output).void.liftConnectionIO

    def insertMany(outputs: List[UOutput]): D[Unit] =
      QS.insertManyNoConflict(outputs).void.liftConnectionIO

    def getByBoxId(boxId: BoxId): D[Option[ExtendedUOutput]] =
      QS.get(boxId).option.liftConnectionIO

    def streamAll(offset: Int, limit: Int): Stream[D, ExtendedUOutput] =
      QS.getAll(offset, limit).stream.translate(LiftConnectionIO[D].liftConnectionIOK)

    def streamAllRelatedToErgoTree(ergoTree: ErgoTree): Stream[D, UOutput] =
      QS.getAllRelatedToErgoTree(ergoTree.value)
        .stream
        .translate(LiftConnectionIO[D].liftConnectionIOK)

    def streamAllUnspent(offset: Int, limit: Int): Stream[D, UOutput] =
      QS.getAllUnspent(offset, limit).stream.translate(LiftConnectionIO[D].liftConnectionIOK)

    def getAllByTxId(txId: TxId): D[List[ExtendedUOutput]] =
      QS.getAllByTxId(txId).to[List].liftConnectionIO

    def getAllByTxIds(txIds: NonEmptyList[TxId]): D[List[ExtendedUOutput]] =
      QS.getAllByTxIds(txIds).to[List].liftConnectionIO

    def getAllByErgoTree(ergoTree: HexString): D[List[ExtendedUOutput]] =
      QS.getAllByErgoTree(ergoTree).to[List].liftConnectionIO

    def getAllUnspentByErgoTree(ergoTree: HexString): D[List[ExtendedUOutput]] =
      QS.getAllUnspentByErgoTree(ergoTree).to[List].liftConnectionIO

    def streamAllUnspentByErgoTree(
      ergoTree: HexString,
      offset: Int,
      limit: Int,
      ordering: OrderingString
    ): Stream[D, AnyOutput] =
      QS.streamAllUnspentByErgoTree(ergoTree, offset, limit, ordering)
        .stream
        .translate(LiftConnectionIO[D].liftConnectionIOK)

    def sumUnspentByErgoTree(ergoTree: HexString): D[Long] =
      QS.sumUnspentByErgoTree(ergoTree).unique.liftConnectionIO

    def countUnspentByErgoTree(ergoTree: HexString): D[Int] =
      QS.countUnspentByErgoTree(ergoTree).unique.liftConnectionIO

    def countAllByErgoTree(ergoTree: HexString): D[Int] =
      QS.countAllByErgoTree(ergoTree).unique.liftConnectionIO
  }
}
