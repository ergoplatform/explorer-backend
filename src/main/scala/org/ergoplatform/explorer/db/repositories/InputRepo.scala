package org.ergoplatform.explorer.db.repositories

import cats.Functor
import cats.data.NonEmptyList
import cats.implicits._
import doobie.refined.implicits._
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.algebra.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.models.Input
import org.ergoplatform.explorer.db.models.composite.ExtendedInput
import org.ergoplatform.explorer.db.doobieInstances._

/** [[Input]] and [[ExtendedInput]] data access operations.
  */
trait InputRepo[D[_]] {

  /** Put a given `input` to persistence.
    */
  def insert(input: Input): D[Unit]

  /** Get all inputs related to a given `txId`.
    */
  def getAllByTxId(txId: TxId): D[List[ExtendedInput]]

  /** Get all inputs related to a given list of `txId`.
    */
  def getAllByTxIds(txsId: NonEmptyList[TxId]): D[List[ExtendedInput]]
}

object InputRepo {

  def apply[D[_]: LiftConnectionIO: Functor]: InputRepo[D] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO: Functor] extends InputRepo[D] {

    import org.ergoplatform.explorer.db.queries.{InputQuerySet => QS}

    def insert(input: Input): D[Unit] =
      QS.insert(input).liftConnectionIO.void

    def getAllByTxId(txId: TxId): D[List[ExtendedInput]] =
      QS.getAllByTxId(txId).liftConnectionIO

    def getAllByTxIds(txIds: NonEmptyList[TxId]): D[List[ExtendedInput]] =
      QS.getAllByTxIds(txIds).liftConnectionIO
  }
}
