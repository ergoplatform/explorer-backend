package org.ergoplatform.explorer.grabber

import cats.effect.{ExitCode, Resource}
import cats.syntax.functor._
import doobie.free.connection.ConnectionIO
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monix.eval.{Task, TaskApp}
import org.ergoplatform.explorer.context.GrabberContext
import tofu.{Context, HasContext}

import scala.concurrent.ExecutionContext.Implicits.global

/** A service dumping blocks from the Ergo network to local db.
  */
object Application extends TaskApp {

  def run(args: List[String]): Task[ExitCode] =
    resources(args.headOption).use {
      case (logger, ctx) =>
        implicit val c: HasContext[Task, GrabberContext[Task, ConnectionIO]] = Context.const(ctx)
        logger.info("Starting Grabber service ..") >>
        ChainGrabber[Task, ConnectionIO]
          .flatMap(_.run.compile.drain)
          .as(ExitCode.Success)
          .guarantee(logger.info("Stopping Grabber service .."))
    }

  private def resources(configPathOpt: Option[String]) =
    for {
      logger <- Resource.liftF(Slf4jLogger.create)
      ctx    <- GrabberContext.make[Task](configPathOpt)
    } yield (logger, ctx)
}
