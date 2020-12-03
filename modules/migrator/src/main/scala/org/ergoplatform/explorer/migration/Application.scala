package org.ergoplatform.explorer.migration

import cats.effect.{ExitCode, IO, IOApp, Resource}
import doobie.util.transactor.Transactor
import org.ergoplatform.explorer.db.DoobieTrans
import org.ergoplatform.explorer.migration.migrations.RegistersMigration
import pureconfig.generic.auto._
import tofu.syntax.console._
import tofu.syntax.monadic._

import scala.concurrent.duration._

object Application extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    resources(args.headOption).use { xa =>
      val migrations = makeMigrations(xa)
      putStrLn[IO]("Enter migration ID:") >>
      readStrLn[IO]
        .flatMap { in =>
          migrations(in)
        }
        .as(ExitCode.Success)
    }

  private def resources(configPathOpt: Option[String]) =
    for {
      settings <- Resource.liftF(MigrationConfig.load[IO](configPathOpt))
      xa       <- DoobieTrans[IO]("Migrator", settings.db)
    } yield xa

  private def makeMigrations(xa: Transactor[IO]) =
    Map("v4v5" -> RegistersMigration(RegistersMigrationConfig(batchSize = 100, interval = 1.second), xa))
}
