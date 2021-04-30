package org.ergoplatform.explorer.migration

import cats.effect.{ExitCode, IO, IOApp, Resource}
import doobie.util.transactor.Transactor
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.db.DoobieTrans
import org.ergoplatform.explorer.migration.configs.{MigrationConfig, ProcessingConfig}
import org.ergoplatform.explorer.migration.migrations.{AssetsMigration, BlockchainStatsMigration, RegistersAndConstantsMigration, RegistersMigration}
import org.ergoplatform.explorer.settings.pureConfigInstances._
import pureconfig.generic.auto._
import tofu.syntax.console._

object Application extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    resources(args.lift(1)).use { case (xa, conf) =>
      val migrations = makeMigrations(conf.processing, xa)(conf.addressEncoder)
      args.headOption match {
        case Some(migrationId) =>
          migrations(migrationId) as ExitCode.Success
        case None =>
          putErrLn[IO]("Migration ID must be provided via program argument") as ExitCode.Error
      }
    }

  private def resources(configPathOpt: Option[String]) =
    for {
      settings <- Resource.eval(MigrationConfig.load[IO](configPathOpt))
      xa       <- DoobieTrans[IO]("Migrator", settings.db)
    } yield xa -> settings

  private def makeMigrations(conf: ProcessingConfig, xa: Transactor[IO])(implicit e: ErgoAddressEncoder) =
    Map(
      "v4v5" -> RegistersMigration(conf, xa),
      "v5v6" -> AssetsMigration(conf, xa),
      "v6v7" -> RegistersAndConstantsMigration(conf, xa),
      "blockchainStat" -> BlockchainStatsMigration(conf, xa)
    )
}
