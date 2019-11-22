package org.ergoplatform.explorer.db.repositories

import cats.Functor
import cats.implicits._
import doobie.util.transactor.Transactor
import doobie.implicits._
import fs2.Stream
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.{Address, AssetId, BoxId}
import org.ergoplatform.explorer.db.models.Asset
import org.ergoplatform.explorer.db.algebra.syntax.liftConnectionIO._

/** [[Asset]] data access operations.
  */
trait AssetRepo[D[_], G[_]] {

  /** Put a given `asset` to persistence.
    */
  def insert(asset: Asset): D[Unit]

  /** Get all assets belonging to a given `boxId`.
    */
  def getAllByBoxId(boxId: BoxId): D[List[Asset]]

  /** Get all addresses holding an asset with a given `assetId`.
    */
  def getAllHoldingAddresses(
    assetId: AssetId,
    offset: Int,
    limit: Int
  ): G[Address]
}

object AssetRepo {

  def apply[D[_]: LiftConnectionIO: Functor]: AssetRepo[D, Stream[D, *]] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO: Functor]
    extends AssetRepo[D, Stream[D, *]] {

    import org.ergoplatform.explorer.db.queries.{AssetQuerySet => QS}

    def insert(asset: Asset): D[Unit] =
      QS.insert(asset).liftConnectionIO.void

    def getAllByBoxId(boxId: BoxId): D[List[Asset]] =
      QS.getAllByBoxId(boxId).liftConnectionIO

    def getAllHoldingAddresses(
      assetId: AssetId,
      offset: Int,
      limit: Int
    ): Stream[D, Address] =
      QS.getAllHoldingAddresses(assetId, offset, limit)
        .translate(LiftConnectionIO[D].liftConnectionIOK)
  }
}
