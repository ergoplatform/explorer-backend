package org.ergoplatform.explorer.indexer

import cats.effect.{ExitCode, Resource}
import cats.syntax.functor._
import doobie.free.connection.ConnectionIO
import fs2.Stream
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monix.eval.{Task, TaskApp}
import org.ergoplatform.explorer.cache.Redis
import org.ergoplatform.explorer.services.ErgoNetwork
import org.ergoplatform.explorer.db.{DoobieTrans, Trans}
import org.ergoplatform.explorer.indexer.cache.ApiQueryCache
import org.ergoplatform.explorer.indexer.processes.{ChainIndexer, EpochsIndexer}
import org.ergoplatform.explorer.settings.pureConfigInstances._
import org.ergoplatform.explorer.settings.IndexerSettings
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import pureconfig.generic.auto._
import tofu.concurrent.MakeRef
import tofu.logging.Logs
import cats.effect.IO

import scala.concurrent.ExecutionContext.global

/** A service dumping blocks from the Ergo network to local db.
  */
object Application extends TaskApp {

  implicit val logs: Logs[Task, Task]       = Logs.sync
  implicit val makeRef: MakeRef[Task, Task] = MakeRef.syncInstance

  def run(args: List[String]): Task[ExitCode] =
    resources(args.headOption).use[Task, ExitCode] { case (logger, settings, client, trans, redis) =>
      logger.info("Starting Indexers ..") >>
        ErgoNetwork[Task](client, settings.network)
          .flatMap { ns =>
            val cache = ApiQueryCache.make(redis)
            mkProgram(ns, settings, trans, cache).compile.drain as ExitCode.Success
          }
          .guarantee(logger.info("Stopping Chain Indexer ..."))
    }

  private def resources(configPathOpt: Option[String]) =
    for {
      logger   <- Resource.eval(Slf4jLogger.create)
      settings <- Resource.eval(IndexerSettings.load[Task](configPathOpt))
      client   <- BlazeClientBuilder[Task](global).resource
      xa       <- DoobieTrans[Task]("IndexerPool", settings.db)
      trans = Trans.fromDoobie(xa)
      redis <- Redis[Task](settings.redisCache)
    } yield (logger, settings, client, trans, redis)

  private def mkProgram(
    network: ErgoNetwork[Task],
    settings: IndexerSettings,
    trans: ConnectionIO Trans Task,
    cache: ApiQueryCache[Task]
  ): Stream[Task, Unit] =
    Stream
      .emits(
        List(
          Stream.eval(ChainIndexer[Task, ConnectionIO](settings, network, cache)(trans)).flatMap(_.run),
          Stream.eval(EpochsIndexer[Task, ConnectionIO](settings, network)(trans)).flatMap(_.run)
        )
      )
      .parJoinUnbounded
}
