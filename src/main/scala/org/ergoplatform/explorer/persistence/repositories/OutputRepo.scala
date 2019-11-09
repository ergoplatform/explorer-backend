package org.ergoplatform.explorer.persistence.repositories

import cats.effect.Sync
import cats.implicits._
import doobie.implicits._
import doobie.util.transactor.Transactor
import fs2.Stream
import org.ergoplatform.explorer.persistence.models.Output
import org.ergoplatform.explorer.persistence.models.composite.ExtendedOutput
import org.ergoplatform.explorer.{Address, BoxId, HexString}

/** [[Output]] and [[ExtendedOutput]] data access operations.
  */
trait OutputRepo[F[_], G[_]] {

  /** Put a given `output` to persistence.
    */
  def insert(output: Output): F[Unit]

  /** Get an output with a given `boxId` from persistence.
    */
  def getByBoxId(boxId: BoxId): F[Option[ExtendedOutput]]

  /** Get all outputs with a given `address` from persistence.
    */
  def getAllByAddress(address: Address): G[ExtendedOutput]

  /** Get all outputs with a given `ergoTree` from persistence.
    */
  def getAllByErgoTree(ergoTree: HexString): G[ExtendedOutput]

  /** Get all unspent main-chain outputs with a given `address` from persistence.
    */
  def getAllMainUnspentByAddress(address: Address): G[ExtendedOutput]

  /** Get all unspent main-chain outputs with a given `ergoTree` from persistence.
    */
  def getAllMainUnspentByErgoTree(ergoTree: HexString): G[ExtendedOutput]

  /** Search for addresses containing a given `substring`.
    */
  def searchAddressesBySubstring(substring: String): F[List[Address]]
}

object OutputRepo {

  final class Live[F[_]: Sync](xa: Transactor[F])
    extends OutputRepo[F, Stream[F, *]] {

    import org.ergoplatform.explorer.persistence.queries.{OutputQuerySet => QS}

    def insert(output: Output): F[Unit] =
      QS.insert(output).transact(xa).as(())

    def getByBoxId(boxId: BoxId): F[Option[ExtendedOutput]] =
      QS.getByBoxId(boxId).transact(xa)

    def getAllByAddress(address: Address): Stream[F, ExtendedOutput] =
      QS.getAllByAddress(address).transact(xa)

    def getAllByErgoTree(ergoTree: HexString): Stream[F, ExtendedOutput] =
      QS.getAllByErgoTree(ergoTree).transact(xa)

    def getAllMainUnspentByAddress(address: Address): Stream[F, ExtendedOutput] =
      QS.getAllMainUnspentByAddress(address).transact(xa)

    def getAllMainUnspentByErgoTree(ergoTree: HexString): Stream[F, ExtendedOutput] =
      QS.getAllMainUnspentByErgoTree(ergoTree).transact(xa)

    def searchAddressesBySubstring(substring: String): F[List[Address]] =
      QS.searchAddressesBySubstring(substring).transact(xa)
  }
}
