package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.syntax.functor._
import doobie.implicits._
import org.ergoplatform.explorer.BoxId
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.UAsset
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._

/** [[UAsset]] data access operations.
  */
trait UAssetRepo[D[_]] {

  /** Put a given `asset` to persistence.
    */
  def insert(asset: UAsset): D[Unit]

  /** Put a given list of assets to persistence.
    */
  def insertMany(assets: List[UAsset]): D[Unit]

  /** Get all assets belonging to a given `boxId`.
    */
  def getAllByBoxId(boxId: BoxId): D[List[UAsset]]

  /** Get all assets belonging to a given list of `boxId`.
   */
  def getAllByBoxIds(boxIds: NonEmptyList[BoxId]): D[List[UAsset]]
}

object UAssetRepo {

  def apply[D[_]: LiftConnectionIO]: UAssetRepo[D] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO] extends UAssetRepo[D] {

    import org.ergoplatform.explorer.db.queries.{UAssetQuerySet => QS}

    def insert(asset: UAsset): D[Unit] =
      QS.insert(asset).void.liftConnectionIO

    def insertMany(assets: List[UAsset]): D[Unit] =
      QS.insertMany(assets).void.liftConnectionIO

    def getAllByBoxId(boxId: BoxId): D[List[UAsset]] =
      QS.getAllByBoxId(boxId).to[List].liftConnectionIO

    def getAllByBoxIds(boxIds: NonEmptyList[BoxId]): D[List[UAsset]] =
      QS.getAllByBoxIds(boxIds).to[List].liftConnectionIO
  }
}
