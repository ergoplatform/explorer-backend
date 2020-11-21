package org.ergoplatform.explorer.http.api

import cats.effect.{ExitCode, Resource}
import cats.free.Free.catsFreeMonadForFree
import cats.syntax.functor._
import doobie.free.connection.ConnectionIO
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monix.eval.{Task, TaskApp}
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.cache.Redis
import org.ergoplatform.explorer.db.{DoobieTrans, Trans}
import org.ergoplatform.explorer.settings.ApiAppSettings
import org.ergoplatform.explorer.http.api.decodingFailureHandler._
import pureconfig.generic.auto._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object Application extends TaskApp {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  def run(args: List[String]): Task[ExitCode] =
    resources(args.headOption).use {
      case (logger, conf, xa, redis) =>
        implicit val e: ErgoAddressEncoder = conf.protocol.addressEncoder
        logger.info("Starting ExplorerApi service ..") >>
        HttpApi[Task, ConnectionIO](conf.http, conf.protocol, conf.utxCache, conf.service, redis)(
          Trans.fromDoobie(xa)
        ).use(_ => Task.never)
          .as(ExitCode.Success)
          .guarantee(logger.info("Stopping ExplorerApi service .."))
    }

  private def resources(configPathOpt: Option[String]) =
    for {
      logger   <- Resource.liftF(Slf4jLogger.create)
      settings <- Resource.liftF(ApiAppSettings.load(configPathOpt))
      tr       <- DoobieTrans[Task]("ApiPool", settings.db)
      redis    <- Redis[Task](settings.redis)
    } yield (logger, settings, tr, redis)
}
