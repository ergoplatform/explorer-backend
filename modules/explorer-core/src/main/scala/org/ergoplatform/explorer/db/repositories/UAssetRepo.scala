package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.functor._
import doobie.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.UAsset
import org.ergoplatform.explorer.db.models.aggregates.{AggregatedAsset, AnyAsset, ExtendedUAsset}
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
  def getAllByBoxId(boxId: BoxId): D[List[ExtendedUAsset]]

  /** Get all assets belonging to a given list of `boxId`.
    */
  def getAllByBoxIds(boxIds: NonEmptyList[BoxId]): D[List[ExtendedUAsset]]

  /** Get confirmed + unconfirmed assets belonging to a given list of `boxId`.
    */
  def getConfirmedAndUnconfirmed(boxIds: NonEmptyList[BoxId]): D[List[AnyAsset]]

  /** Get all unspent assets belonging to a given `address`.
    */
  def getAllUnspentByErgoTree(ergoTree: HexString): D[List[ExtendedUAsset]]

  def aggregateUnspentByErgoTree(ergoTree: HexString): D[List[AggregatedAsset]]
}

object UAssetRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[UAssetRepo[D]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler) extends UAssetRepo[D] {

    import org.ergoplatform.explorer.db.queries.{UAssetQuerySet => QS}

    def insert(asset: UAsset): D[Unit] =
      QS.insertNoConflict(asset).void.liftConnectionIO

    def insertMany(assets: List[UAsset]): D[Unit] =
      QS.insertManyNoConflict(assets).void.liftConnectionIO

    def getAllByBoxId(boxId: BoxId): D[List[ExtendedUAsset]] =
      QS.getAllByBoxId(boxId).to[List].liftConnectionIO

    def getAllByBoxIds(boxIds: NonEmptyList[BoxId]): D[List[ExtendedUAsset]] =
      QS.getAllByBoxIds(boxIds).to[List].liftConnectionIO

    def getConfirmedAndUnconfirmed(boxIds: NonEmptyList[BoxId]): D[List[AnyAsset]] =
      QS.getConfirmedAndUnconfirmed(boxIds).to[List].liftConnectionIO

    def getAllUnspentByErgoTree(ergoTree: HexString): D[List[ExtendedUAsset]] =
      QS.getAllUnspentByErgoTree(ergoTree).to[List].liftConnectionIO

    def aggregateUnspentByErgoTree(ergoTree: HexString): D[List[AggregatedAsset]] =
      QS.aggregateUnspentByErgoTree(ergoTree).to[List].liftConnectionIO
  }
}
