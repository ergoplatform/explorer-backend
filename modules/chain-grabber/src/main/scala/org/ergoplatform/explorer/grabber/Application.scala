package org.ergoplatform.explorer.grabber

import cats.effect.{ExitCode, Resource}
import cats.syntax.functor._
import mouse.anyf._
import doobie.free.connection.ConnectionIO
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monix.eval.{Task, TaskApp}
import org.ergoplatform.explorer.context._
import tofu.{Context, HasContext}
import doobie.free.connection.AsyncConnectionIO
import org.ergoplatform.explorer.clients.ergo.ErgoNetworkClient
import org.ergoplatform.explorer.db.{DoobieTrans, Trans}
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext.Implicits.global

/** A service dumping blocks from the Ergo network to local db.
  */
object Application extends TaskApp {

  def run(args: List[String]): Task[ExitCode] =
    resources(args.headOption).use {
      case (logger, ctx, repos) =>
        implicit val c: HasContext[Task, GrabberContext[Task, ConnectionIO]] = Context.const(ctx)
        implicit val r: HasContext[ConnectionIO, RepositoryContext[ConnectionIO, fs2.Stream]] =
          Context.const(repos)
        logger.info("Starting Grabber service ..") >>
        ChainGrabber[Task, ConnectionIO]
          .flatMap(_.run.compile.drain)
          .as(ExitCode.Success)
          .guarantee(logger.info("Stopping Grabber service .."))
    }

  private def resources(configPathOpt: Option[String]) =
    for {
      logger   <- Resource.liftF(Slf4jLogger.create[Task])
      settings <- Resource.liftF(SettingsContext.make[Task](configPathOpt))
      repos    <- Resource.liftF(RepositoryContext.make[Task, ConnectionIO])
      client   <- BlazeClientBuilder[Task](global).resource
      xa       <- DoobieTrans[Task]("GrabberPool", settings.db)
      ns       <- Resource.liftF(ErgoNetworkClient[Task](client, settings.masterNodesAddresses))
      ctx = GrabberContext(settings, repos, ns, Trans.fromDoobie(xa))
      repos <- Resource.liftF(RepositoryContext.make[ConnectionIO, ConnectionIO] ||> xa.trans)
    } yield (logger, ctx, repos)
}
