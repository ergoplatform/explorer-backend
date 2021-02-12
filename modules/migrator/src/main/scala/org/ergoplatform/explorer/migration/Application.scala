package org.ergoplatform.explorer.migration

import cats.effect.{ExitCode, IO, IOApp, Resource}
import doobie.util.transactor.Transactor
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.db.DoobieTrans
import org.ergoplatform.explorer.migration.configs.{AssetsMigrationConfig, MigrationConfig, RegistersMigrationConfig}
import org.ergoplatform.explorer.migration.migrations.{
  AssetsMigration,
  RegistersAndConstantsMigration,
  RegistersMigration
}
import org.ergoplatform.explorer.settings.pureConfigInstances._
import pureconfig.generic.auto._
import tofu.syntax.console._

import scala.concurrent.duration._

object Application extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    resources(args.lift(1)).use { case (xa, conf) =>
      val migrations = makeMigrations(conf.offset, xa)(conf.addressEncoder)
      args.headOption match {
        case Some(migrationId) =>
          migrations(migrationId) as ExitCode.Success
        case None =>
          putErrLn[IO]("Migration ID must be provided via program argument") as ExitCode.Error
      }
    }

  private def resources(configPathOpt: Option[String]) =
    for {
      settings <- Resource.liftF(MigrationConfig.load[IO](configPathOpt))
      xa       <- DoobieTrans[IO]("Migrator", settings.db)
    } yield xa -> settings

  private def makeMigrations(offset: Int, xa: Transactor[IO])(implicit e: ErgoAddressEncoder) =
    Map(
      "v4v5" -> RegistersMigration(RegistersMigrationConfig(batchSize = 1000, interval = 500.millis, offset), xa),
      "v5v6" -> AssetsMigration(AssetsMigrationConfig(batchSize = 1000, interval = 500.millis, offset), xa),
      "v6v7" -> RegistersAndConstantsMigration(
        RegistersMigrationConfig(batchSize = 1000, interval = 500.millis, offset),
        xa
      )
    )
}
