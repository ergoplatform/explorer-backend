package org.ergoplatform.explorer.db.repositories

import cats.syntax.functor._
import fs2.Stream
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.UInput
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._

/** [[UInput]] data access operations.
  */
trait UInputRepo[D[_], S[_[_], _]] {

  /** Put a given `input` to persistence.
    */
  def insert(input: UInput): D[Unit]

  /** Put a given list of inputs to persistence.
    */
  def insetMany(inputs: List[UInput]): D[Unit]

  /** Get all inputs containing in unconfirmed transactions.
    */
  def getAll(offset: Int, limit: Int): S[D, UInput]

  /** Get all inputs related to transaction with a given `txId`.
    */
  def getAllByTxId(txId: TxId): D[List[UInput]]
}

object UInputRepo {

  def apply[D[_]: LiftConnectionIO]: UInputRepo[D, Stream] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO] extends UInputRepo[D, Stream] {

    import org.ergoplatform.explorer.db.queries.{UInputQuerySet => QS}

    def insert(input: UInput): D[Unit] =
      QS.insert(input).void.liftConnectionIO

    def insetMany(inputs: List[UInput]): D[Unit] =
      QS.insertMany(inputs).void.liftConnectionIO

    def getAll(offset: Int, limit: Int): Stream[D, UInput] =
      QS.getAll(offset, limit).translate(LiftConnectionIO[D].liftConnectionIOK)

    def getAllByTxId(txId: TxId): D[List[UInput]] =
      QS.getAllByTxId(txId).liftConnectionIO
  }
}
