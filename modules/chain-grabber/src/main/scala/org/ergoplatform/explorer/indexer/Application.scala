package org.ergoplatform.explorer.indexer

import cats.effect.{ExitCode, Resource}
import cats.syntax.functor._
import doobie.free.connection.ConnectionIO
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monix.eval.{Task, TaskApp}
import org.ergoplatform.explorer.clients.ergo.ErgoNetworkClient
import org.ergoplatform.explorer.db.DoobieTrans
import org.ergoplatform.explorer.indexer.processes.ChainIndexing
import org.ergoplatform.explorer.settings.GrabberAppSettings
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext.global

/** A service dumping blocks from the Ergo network to local db.
  */
object Application extends TaskApp {

  def run(args: List[String]): Task[ExitCode] =
    resources(args.headOption).use {
      case (logger, settings, client, xa) =>
        logger.info("Starting Indexer service ..") >>
        ErgoNetworkClient[Task](client, settings.masterNodesAddresses).flatMap { ns =>
          ChainIndexing[Task, ConnectionIO](settings, ns)(xa)
            .flatMap(_.run.compile.drain)
            .as(ExitCode.Success)
        }.guarantee(logger.info("Stopping Indexer service .."))
    }

  private def resources(configPathOpt: Option[String]) =
    for {
      logger   <- Resource.liftF(Slf4jLogger.create)
      settings <- Resource.liftF(GrabberAppSettings.load(configPathOpt))
      client   <- BlazeClientBuilder[Task](global).resource
      xa       <- DoobieTrans[Task]("IndexerPool", settings.db).map(_.trans)
    } yield (logger, settings, client, xa)
}
