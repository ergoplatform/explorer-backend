package org.ergoplatform.explorer.broadcaster

import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, Sync, Timer}
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import dev.profunktor.redis4cats.algebra.RedisCommands
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.explorer.Err.RequestProcessingErr.NetworkErr.InvalidTransaction
import org.ergoplatform.explorer.cache.repositories.ErgoLikeTransactionRepo
import org.ergoplatform.explorer.services.ErgoNetwork
import org.ergoplatform.explorer.settings.UtxBroadcasterSettings

/** Broadcasts new transactions to the network.
  */
final class UtxBroadcaster[F[_]: Timer: Sync: Logger](
  settings: UtxBroadcasterSettings,
  network: ErgoNetwork[F],
  repo: ErgoLikeTransactionRepo[F, Stream]
) {

  private val log = Logger[F]

  def run: Stream[F, Unit] =
    Stream(()).repeat
      .covary[F]
      .metered(settings.tickInterval)
      .flatMap(_ => broadcastPool)
      .handleErrorWith { e =>
        Stream.eval(log.error(e)(s"An error occurred while broadcasting local utx pool")) >> run
      }

  private def broadcastPool: Stream[F, Unit] =
    Stream
      .eval(Ref.of(0))
      .flatTap { count =>
        repo.getAll.evalMap { tx =>
          log.info(s"Broadcasting transaction ${tx.id}") >>
          count.update(_ + 1) >>
          network
            .submitTransaction(tx)
            .recoverWith { case _: InvalidTransaction =>
              log.info(s"Transaction ${tx.id} was invalidated")
            } >>
          repo.delete(tx.id)
        }
      }
      .flatMap { cRef =>
        Stream.eval(cRef.get >>= (c => log.info(s"$c transactions processed")))
      }
}

object UtxBroadcaster {

  def apply[F[_]: Timer: Concurrent](
    settings: UtxBroadcasterSettings,
    network: ErgoNetwork[F],
    redis: RedisCommands[F, String, String]
  ): F[UtxBroadcaster[F]] =
    Slf4jLogger.create.flatMap { implicit logger =>
      ErgoLikeTransactionRepo[F](settings.utxCache, redis)
        .map(new UtxBroadcaster(settings, network, _))
    }
}
