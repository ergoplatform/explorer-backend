package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.functor._
import doobie.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.LiftConnectionIO
import org.ergoplatform.explorer.db.models.UAsset
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.{BoxId, HexString}

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

  /** Get all unspent assets belonging to a given `address`.
    */
  def getAllUnspentByErgoTree(ergoTree: HexString): D[List[UAsset]]
}

object UAssetRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[UAssetRepo[D]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler) extends UAssetRepo[D] {

    import org.ergoplatform.explorer.db.queries.{UAssetQuerySet => QS}

    def insert(asset: UAsset): D[Unit] =
      QS.insert(asset).void.liftConnIO

    def insertMany(assets: List[UAsset]): D[Unit] =
      QS.insertMany(assets).void.liftConnIO

    def getAllByBoxId(boxId: BoxId): D[List[UAsset]] =
      QS.getAllByBoxId(boxId).to[List].liftConnIO

    def getAllByBoxIds(boxIds: NonEmptyList[BoxId]): D[List[UAsset]] =
      QS.getAllByBoxIds(boxIds).to[List].liftConnIO

    def getAllUnspentByErgoTree(ergoTree: HexString): D[List[UAsset]] =
      QS.getAllUnspentByErgoTree(ergoTree).to[List].liftConnIO
  }
}
