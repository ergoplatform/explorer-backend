package org.ergoplatform.explorer

import cats.effect.Timer
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.list._
import cats.syntax.traverse._
import cats.{~>, Applicative, Monad}
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import mouse.anyf._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.db.models.aggregates.FlatUTransaction
import org.ergoplatform.explorer.db.repositories.{
  UAssetRepo,
  UInputRepo,
  UOutputRepo,
  UTransactionRepo
}
import org.ergoplatform.explorer.services.ErgoNetworkService
import org.ergoplatform.explorer.settings.Settings
import org.ergoplatform.explorer.syntax.stream._

/** Synchronises local memory pool representation with the network.
  */
final class UtxWatcher[
  F[_]: Timer: Applicative: Logger,
  D[_]: Monad
](
  settings: Settings,
  networkService: ErgoNetworkService[F, Stream],
  txRepo: UTransactionRepo[D, Stream],
  inRepo: UInputRepo[D, Stream],
  outRepo: UOutputRepo[D, Stream],
  assetRep: UAssetRepo[D]
)(xa: D ~> F) {

  implicit private val enc: ErgoAddressEncoder = settings.protocol.addressEncoder

  def run: Stream[F, Unit] =
    Stream(()).repeat
      .covary[F]
      .metered(settings.utxPoolPollInterval)
      .flatMap(_ => syncPool)

  private def syncPool: Stream[F, Unit] =
    for {
      _        <- Logger[F].info("Syncing UTX pool ..").asStream
      knownIds <- (txRepo.getAllIds ||> xa).asStream.map(_.toSet)
      txs      <- networkService.getUnconfirmedTransactions.chunkN(100).map(_.toList)
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
