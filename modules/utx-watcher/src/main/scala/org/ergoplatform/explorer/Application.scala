package org.ergoplatform.explorer

import cats.effect.{ExitCode, Resource}
import cats.syntax.functor._
import cats.free.Free.catsFreeMonadForFree
import doobie.free.connection.ConnectionIO
import monix.eval.{Task, TaskApp}
import org.ergoplatform.explorer.db.DoobieTrans
import org.ergoplatform.explorer.services.ErgoNetworkService
import org.ergoplatform.explorer.settings.UtxWatcherSettings
import org.ergoplatform.explorer.watcher.UtxWatcher
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext.global

object Application extends TaskApp {

  def run(args: List[String]): Task[ExitCode] =
    resources(args.headOption).use {
      case (settings, client, xa) =>
        ErgoNetworkService[Task](client, settings.masterNodesAddresses).flatMap { ns =>
          UtxWatcher[Task, ConnectionIO](settings, ns)(xa)
            .flatMap(_.run.compile.drain)
            .as(ExitCode.Success)
        }
    }

  private def resources(configPathOpt: Option[String]) =
    for {
      settings <- Resource.liftF(UtxWatcherSettings.load(configPathOpt))
      client   <- BlazeClientBuilder[Task](global).resource
      xa       <- DoobieTrans[Task]("UtxWatcherPool", settings.db).map(_.trans)
    } yield (settings, client, xa)
}
