package org.ergoplatform.explorer.db.repositories

import cats.implicits._
import doobie.implicits._
import fs2.Stream
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.algebra.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.models.Asset
import org.ergoplatform.explorer.{Address, AssetId, BoxId}

/** [[Asset]] data access operations.
  */
trait AssetRepo[D[_], S[_[_], _]] {

  /** Put a given `asset` to persistence.
    */
  def insert(asset: Asset): D[Unit]

  /** Put a given list of assets to persistence.
    */
  def insertMany(assets: List[Asset]): D[Unit]

  /** Get all assets belonging to a given `boxId`.
    */
  def getAllByBoxId(boxId: BoxId): D[List[Asset]]

  /** Get all addresses holding an asset with a given `assetId`.
    */
  def getAllHoldingAddresses(
    assetId: AssetId,
    offset: Int,
    limit: Int
  ): S[D, Address]
}

object AssetRepo {

  def apply[D[_]: LiftConnectionIO]: AssetRepo[D, Stream] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO] extends AssetRepo[D, Stream] {

    import org.ergoplatform.explorer.db.queries.{AssetQuerySet => QS}

    def insert(asset: Asset): D[Unit] =
      QS.insert(asset).void.liftConnectionIO

    def insertMany(assets: List[Asset]): D[Unit] =
      QS.insertMany(assets).void.liftConnectionIO

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
