package org.ergoplatform.explorer.watcher

import cats.effect.{ExitCode, Resource}
import cats.syntax.functor._
import cats.free.Free.catsFreeMonadForFree
import doobie.free.connection.ConnectionIO
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monix.eval.{Task, TaskApp}
import org.ergoplatform.explorer.clients.ergo.ErgoNetworkClient
import org.ergoplatform.explorer.db.DoobieTrans
import org.ergoplatform.explorer.settings.UtxWatcherSettings
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext.global

object Application extends TaskApp {

  def run(args: List[String]): Task[ExitCode] =
    resources(args.headOption).use {
      case (logger, settings, client, xa) =>
        logger.info("Starting UtxGrabber service ..") >>
        ErgoNetworkClient[Task](client, settings.masterNodesAddresses)
          .flatMap { ns =>
            UtxWatcher[Task, ConnectionIO](settings, ns)(xa)
              .flatMap(_.run.compile.drain)
              .as(ExitCode.Success)
          }
          .guarantee(logger.info("Stopping UtxGrabber service .."))
    }

  private def resources(configPathOpt: Option[String]) =
    for {
      logger   <- Resource.liftF(Slf4jLogger.create)
      settings <- Resource.liftF(UtxWatcherSettings.load(configPathOpt))
      client   <- BlazeClientBuilder[Task](global).resource
      xa       <- DoobieTrans[Task]("UtxWatcherPool", settings.db).map(_.trans)
    } yield (logger, settings, client, xa)
}
