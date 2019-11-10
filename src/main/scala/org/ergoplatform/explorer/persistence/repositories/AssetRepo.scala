package org.ergoplatform.explorer.persistence.repositories

import cats.effect.Sync
import cats.implicits._
import doobie.util.transactor.Transactor
import doobie.implicits._
import fs2.Stream
import org.ergoplatform.explorer.{Address, AssetId, BoxId}
import org.ergoplatform.explorer.persistence.models.Asset

/** [[Asset]] data access operations.
  */
trait AssetRepo[F[_], G[_]] {

  /** Put a given `asset` to persistence.
    */
  def insert(asset: Asset): F[Unit]

  /** Get all assets belonging to a given `boxId`.
    */
  def getAllByBoxId(boxId: BoxId): F[List[Asset]]

  /** Get all addresses holding an asset with a given `assetId`.
    */
  def getAllHoldingAddresses(
    assetId: AssetId,
    offset: Int,
    limit: Int
  ): G[Address]
}

object AssetRepo {

  final class Live[F[_]: Sync](xa: Transactor[F])
    extends AssetRepo[F, Stream[F, *]] {

    import org.ergoplatform.explorer.persistence.queries.{AssetQuerySet => QS}

    def insert(asset: Asset): F[Unit] =
      QS.insert(asset).transact(xa).as(())

    def getAllByBoxId(boxId: BoxId): F[List[Asset]] =
      QS.getAllByBoxId(boxId).transact(xa)

    def getAllHoldingAddresses(
      assetId: AssetId,
      offset: Int,
      limit: Int
    ): Stream[F, Address] =
      QS.getAllHoldingAddresses(assetId, offset, limit).transact(xa)
  }
}
