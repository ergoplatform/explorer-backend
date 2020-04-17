package org.ergoplatform.explorer.broadcaster

import cats.effect.{Concurrent, Sync, Timer}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import dev.profunktor.redis4cats.algebra.RedisCommands
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.explorer.Err.RequestProcessingErr.NetworkErr.InvalidTransaction
import org.ergoplatform.explorer.cache.repositories.ErgoLikeTransactionRepo
import org.ergoplatform.explorer.clients.ergo.ErgoNetworkClient
import org.ergoplatform.explorer.settings.UtxBroadcasterSettings

/** Broadcasts new transactions to the network.
  */
final class UtxBroadcaster[F[_]: Timer: Sync: Logger](
  settings: UtxBroadcasterSettings,
  network: ErgoNetworkClient[F, Stream],
  repo: ErgoLikeTransactionRepo[F, Stream]
) {

  def run: Stream[F, Unit] =
    Stream(()).repeat
      .covary[F]
      .metered(settings.tickInterval)
      .flatMap(_ => broadcastPool)
      .handleErrorWith { e =>
        Stream.eval(Logger[F].error(e)(s"An error occurred while broadcasting local utx pool")) >> run
      }

  private def broadcastPool: Stream[F, Unit] =
    Stream.eval(Logger[F].info(s"Broadcast transactions ..")) >>
    repo.getAll.evalMap { tx =>
      Logger[F].info(s"Broadcasting transaction ${tx.id}") >>
      network
        .submitTransaction(tx)
        .recoverWith { case _: InvalidTransaction => ().pure } >>
      repo.delete(tx.id)
    }
}

object UtxBroadcaster {

  def apply[F[_]: Timer: Concurrent](
    settings: UtxBroadcasterSettings,
    network: ErgoNetworkClient[F, Stream],
    redis: RedisCommands[F, String, String]
  ): F[UtxBroadcaster[F]] =
    Slf4jLogger.create.flatMap { implicit logger =>
      ErgoLikeTransactionRepo[F](settings.utxCache, redis)
        .map(new UtxBroadcaster(settings, network, _))
    }
}
