package org.ergoplatform.explorer.tracker

import cats.effect.{Sync, Timer}
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.list._
import cats.syntax.traverse._
import cats.{~>, Monad}
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import mouse.anyf._
import org.ergoplatform.explorer.BuildFrom.syntax._
import org.ergoplatform.explorer.services.ErgoNetwork
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.aggregates.FlatUTransaction
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.settings.{ProtocolSettings, UtxTrackerSettings}
import org.ergoplatform.explorer.tracker.extractors._
import tofu.{Context, MonadThrow, WithContext}

/** Synchronises local memory pool representation with the network.
  */
final class UtxTracker[
  F[_]: Timer: Logger: MonadThrow,
  D[_]: Monad
](
   settings: UtxTrackerSettings,
   network: ErgoNetwork[F],
   txRepo: UTransactionRepo[D, Stream],
   inRepo: UInputRepo[D, Stream],
   dataInRepo: UDataInputRepo[D, Stream],
   outRepo: UOutputRepo[D, Stream],
   assetRep: UAssetRepo[D]
)(xa: D ~> F) {

  implicit private val ctx: WithContext[F, ProtocolSettings] =
    Context.const[F, ProtocolSettings](settings.protocol)

  def run: Stream[F, Unit] =
    Stream(()).repeat
      .covary[F]
      .metered(settings.pollInterval)
      .evalMap { _ =>
        Logger[F].info("Syncing UTX pool ..") >>
        syncPool.handleErrorWith { e =>
          Logger[F].warn(e)("An error occurred while syncing with the network. Restarting ...")
        }
      }

  private def syncPool: F[Unit] =
    for {
      knownIds <- (txRepo.getAllIds ||> xa).map(_.toSet)
      txs      <- network.getUnconfirmedTransactions.map(_.toList)
      newTxs  = txs.filterNot(tx => knownIds.contains(tx.id))
      dropIds = knownIds.diff(txs.map(_.id).toSet).toList.toNel
      flatTxs <- newTxs.traverse(_.intoF[F, FlatUTransaction])
      _       <- dropIds.fold(().pure[D])(txRepo.dropMany(_).void) >> writeFlatBatch(flatTxs) ||> xa
      _       <- Logger[F].info(s"${newTxs.size} new transactions written, ${dropIds.size} removed")
    } yield ()

  private def writeFlatBatch(txs: List[FlatUTransaction]): D[Unit] =
    txRepo.insertMany(txs.map(_.tx)) >>
    inRepo.insetMany(txs.flatMap(_.inputs)) >>
    dataInRepo.insetMany(txs.flatMap(_.dataInputs)) >>
    outRepo.insertMany(txs.flatMap(_.outputs)) >>
    assetRep.insertMany(txs.flatMap(_.assets))
}

object UtxTracker {

  def apply[F[_]: Timer: Sync, D[_]: Monad: LiftConnectionIO](
    settings: UtxTrackerSettings,
    network: ErgoNetwork[F]
  )(xa: D ~> F): F[UtxTracker[F, D]] =
    Slf4jLogger.create[F].flatMap { implicit logger =>
      (UTransactionRepo[F, D], UInputRepo[F, D], UDataInputRepo[F, D], UOutputRepo[F, D], UAssetRepo[F, D])
        .mapN(new UtxTracker(settings, network, _, _, _, _, _)(xa))
    }
}
