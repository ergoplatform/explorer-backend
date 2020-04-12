package org.ergoplatform.explorer.watcher

import cats.effect.{Sync, Timer}
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.list._
import cats.syntax.apply._
import cats.syntax.traverse._
import cats.{~>, Applicative, Monad}
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import mouse.anyf._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.clients.ergo.ErgoNetworkClient
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.aggregates.FlatUTransaction
import org.ergoplatform.explorer.db.repositories.{
  UAssetRepo,
  UInputRepo,
  UOutputRepo,
  UTransactionRepo
}
import org.ergoplatform.explorer.settings.UtxWatcherSettings
import org.ergoplatform.explorer.syntax.stream._

/** Synchronises local memory pool representation with the network.
  */
final class UtxWatcher[
  F[_]: Timer: Applicative: Logger,
  D[_]: Monad
](
  settings: UtxWatcherSettings,
  network: ErgoNetworkClient[F, Stream],
  txRepo: UTransactionRepo[D, Stream],
  inRepo: UInputRepo[D, Stream],
  outRepo: UOutputRepo[D, Stream],
  assetRep: UAssetRepo[D]
)(xa: D ~> F) {

  implicit private val enc: ErgoAddressEncoder = settings.protocol.addressEncoder

  def run: Stream[F, Unit] =
    Stream(()).repeat
      .covary[F]
      .metered(settings.pollInterval)
      .flatMap { _ =>
        Logger[F].info("Syncing UTX pool ..").asStream >> syncPool.handleErrorWith { e =>
          Logger[F]
            .warn(e)("An error occurred while syncing with the network. Restarting ...")
            .asStream
        }
      }

  private def syncPool: Stream[F, Unit] =
    for {
      knownIds <- (txRepo.getAllIds ||> xa).asStream.map(_.toSet)
      txs      <- network.getUnconfirmedTransactions.chunkN(100).map(_.toList)
      newTxs   = txs.filterNot(tx => knownIds.contains(tx.id))
      dropIds  = knownIds.diff(txs.map(_.id).toSet).toList.toNel
      flatTxs  <- newTxs.traverse(FlatUTransaction.fromApi[F](_)).asStream
      _        <- ((writeFlatBatch(flatTxs) >> dropIds.fold(().pure[D])(txRepo.dropMany(_).void)) ||> xa).asStream
      _        <- Logger[F].info(s"${newTxs.size} new transactions written, ${dropIds.size} removed").asStream
    } yield ()

  private def writeFlatBatch(txs: List[FlatUTransaction]): D[Unit] =
    txRepo.insertMany(txs.map(_.tx)) >>
    inRepo.insetMany(txs.flatMap(_.inputs)) >>
    outRepo.insertMany(txs.flatMap(_.outputs)) >>
    assetRep.insertMany(txs.flatMap(_.assets))
}

object UtxWatcher {

  def apply[F[_]: Timer: Sync, D[_]: Monad: LiftConnectionIO](
    settings: UtxWatcherSettings,
    network: ErgoNetworkClient[F, Stream]
  )(xa: D ~> F): F[UtxWatcher[F, D]] =
    Slf4jLogger.create[F].flatMap { implicit logger =>
      (UTransactionRepo[F, D], UInputRepo[F, D], UOutputRepo[F, D], UAssetRepo[F, D])
        .mapN(new UtxWatcher(settings, network, _, _, _, _)(xa))
    }
}
