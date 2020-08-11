package org.ergoplatform.explorer.grabber

import cats.effect.{Sync, Timer}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{MonadError, Parallel, ~>}
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.explorer.clients.ergo.ErgoNetworkClient
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.grabber.services.GrabberService
import org.ergoplatform.explorer.settings.GrabberAppSettings
import tofu.MonadThrow

/** Fetches new blocks from the network divide them into
  * separate entities and finally puts them into db.
  */
final class ChainGrabber[
  F[_]: Sync: Parallel: Logger: Timer
](settings: GrabberAppSettings, service: GrabberService[F]) {

  def run: Stream[F, Unit] =
    Stream(()).repeat
      .covary[F]
      .metered(settings.pollInterval)
      .flatMap { _ =>
        Stream.eval(Logger[F].info("Starting sync job ..")) >> service.syncAll.handleErrorWith { e =>
          Stream.eval(
            Logger[F].warn(e)(
              "An error occurred while syncing with the network. Restarting ..."
            )
          )
        }
      }
}

object ChainGrabber {

  def apply[
    F[_]: Sync: Parallel: Timer,
    D[_]: LiftConnectionIO: MonadThrow
  ](
    settings: GrabberAppSettings,
    network: ErgoNetworkClient[F]
  )(xa: D ~> F): F[ChainGrabber[F]] =
    Slf4jLogger.create[F].flatMap { implicit logger =>
      GrabberService(settings.protocol, network)(xa) map (new ChainGrabber[F](settings, _))
    }
}
