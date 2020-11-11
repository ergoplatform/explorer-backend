package org.ergoplatform.explorer.migration.v5

import cats.effect.{ExitCode, IO, IOApp, Resource}
import doobie.ConnectionIO
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.explorer.db.DoobieTrans
import org.ergoplatform.explorer.db.repositories.BoxRegisterRepo
import org.ergoplatform.explorer.settings.DbSettings
import pureconfig.generic.auto._

import scala.concurrent.duration._

object Application extends IOApp {

  private val conf = MigrationConfig(batchSize = 100, interval = 1.second)

  def run(args: List[String]): IO[ExitCode] =
    resources(args.headOption).use {
      case (logger, xa) =>
        for {
          repo <- BoxRegisterRepo[IO, ConnectionIO]
          migration = new RegistersMigration(conf, repo, xa, logger)
          _ <- migration.run
        } yield ExitCode.Success
    }

  private def resources(configPathOpt: Option[String]) =
    for {
      logger   <- Resource.liftF(Slf4jLogger.create[IO])
      settings <- Resource.liftF(DbSettings.load[IO](configPathOpt))
      xa       <- DoobieTrans[IO]("V5Migration", settings)
    } yield (logger, xa)
}
