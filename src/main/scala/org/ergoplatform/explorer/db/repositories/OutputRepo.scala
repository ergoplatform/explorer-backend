package org.ergoplatform.explorer.db.repositories

import cats.Functor
import cats.implicits._
import doobie.refined.implicits._
import fs2.Stream
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.algebra.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.models.Output
import org.ergoplatform.explorer.db.models.composite.ExtendedOutput
import org.ergoplatform.explorer.{Address, BoxId, HexString}
import org.ergoplatform.explorer.db.doobieInstances._

/** [[Output]] and [[ExtendedOutput]] data access operations.
  */
trait OutputRepo[D[_], G[_]] {

  /** Put a given `output` to persistence.
    */
  def insert(output: Output): D[Unit]

  /** Get an output with a given `boxId` from persistence.
    */
  def getByBoxId(boxId: BoxId): D[Option[ExtendedOutput]]

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
  def searchAddressesBySubstring(substring: String): D[List[Address]]
}

object OutputRepo {

  def apply[D[_]: LiftConnectionIO: Functor]: OutputRepo[D, Stream[D, *]] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO: Functor]
    extends OutputRepo[D, Stream[D, *]] {

    import org.ergoplatform.explorer.db.queries.{OutputQuerySet => QS}

    private val liftK = LiftConnectionIO[D].liftConnectionIOK

    def insert(output: Output): D[Unit] =
      QS.insert(output).liftConnectionIO.void

    def getByBoxId(boxId: BoxId): D[Option[ExtendedOutput]] =
      QS.getByBoxId(boxId).liftConnectionIO

    def getAllByAddress(address: Address): Stream[D, ExtendedOutput] =
      QS.getAllByAddress(address).translate(liftK)

    def getAllByErgoTree(ergoTree: HexString): Stream[D, ExtendedOutput] =
      QS.getAllByErgoTree(ergoTree).translate(liftK)

    def getAllMainUnspentByAddress(address: Address): Stream[D, ExtendedOutput] =
      QS.getAllMainUnspentByAddress(address).translate(liftK)

    def getAllMainUnspentByErgoTree(ergoTree: HexString): Stream[D, ExtendedOutput] =
      QS.getAllMainUnspentByErgoTree(ergoTree).translate(liftK)

    def searchAddressesBySubstring(substring: String): D[List[Address]] =
      QS.searchAddressesBySubstring(substring).liftConnectionIO
  }
}
