package org.ergoplatform.explorer.grabber

import cats.effect.{ExitCode, Resource}
import cats.syntax.functor._
import doobie.free.connection.{AsyncConnectionIO, ConnectionIO}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monix.eval.{Task, TaskApp}
import org.ergoplatform.explorer.clients.ergo.ErgoNetworkClient
import org.ergoplatform.explorer.context._
import org.ergoplatform.explorer.db.{DoobieTrans, Trans}
import org.http4s.client.blaze.BlazeClientBuilder
import tofu.{Context, HasContext}

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
      client   <- BlazeClientBuilder[Task](global).resource
      xa       <- DoobieTrans[Task]("GrabberPool", settings.db)
      ns       <- Resource.liftF(ErgoNetworkClient[Task](client, settings.masterNodesAddresses))
      ctx = GrabberContext(settings, ns, Trans.fromDoobie(xa))
      repos <- Resource.liftF(RepositoryContext.make[ConnectionIO, ConnectionIO]).mapK(xa.trans)
    } yield (logger, ctx, repos)
}
