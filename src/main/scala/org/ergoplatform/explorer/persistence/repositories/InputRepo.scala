package org.ergoplatform.explorer.persistence.repositories

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.transactor.Transactor
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.persistence.models.Input
import org.ergoplatform.explorer.persistence.models.composite.ExtendedInput
import org.ergoplatform.explorer.persistence.doobieInstances._

/** [[Input]] and [[ExtendedInput]] data access operations.
  */
trait InputRepo[F[_]] {

  /** Put a given `input` to persistence.
    */
  def insert(input: Input): F[Unit]

  /** Get all inputs related to a given `txId`.
    */
  def getAllByTxId(txId: TxId): F[List[ExtendedInput]]

  /** Get all inputs related to a given list of `txId`.
    */
  def getAllByTxIds(txsId: NonEmptyList[TxId]): F[List[ExtendedInput]]
}

object InputRepo {

  final class Live[F[_]: Sync](xa: Transactor[F]) extends InputRepo[F] {

    import org.ergoplatform.explorer.persistence.queries.{InputQuerySet => QS}

    def insert(input: Input): F[Unit] =
      QS.insert(input).transact(xa).as(())

    def getAllByTxId(txId: TxId): F[List[ExtendedInput]] =
      QS.getAllByTxId(txId).transact(xa)

    def getAllByTxIds(txIds: NonEmptyList[TxId]): F[List[ExtendedInput]] =
      QS.getAllByTxIds(txIds).transact(xa)
  }
}
