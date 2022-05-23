package org.ergoplatform.explorer.migration.migrations

import cats.Parallel
import cats.effect.{IO, Timer}
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.{ConnectionIO, Update}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.explorer.migration.configs.ProcessingConfig
import tofu.syntax.monadic._

final class BlockedTokenMigration(
  conf: ProcessingConfig,
  xa: Transactor[IO],
  log: Logger[IO]
) {
  def run: IO[Unit] = updateSchema()

  def updateSchema(): IO[Unit] = {
    val txn = recreateBlockedTokensTable
    log.info("[updating DB schema]") >> txn.transact(xa)
  }

  def recreateBlockedTokensTable: ConnectionIO[Unit] =
    sql"""
         |create table if not exists blocked_tokens
         |(
         |    token_id        VARCHAR(64)   PRIMARY KEY,
         |    token_name      VARCHAR      NOT NULL
         |)
         |""".stripMargin.update.run.void
}

object BlockedTokenMigration {

  def apply(
    conf: ProcessingConfig,
    xa: Transactor[IO]
  )(implicit timer: Timer[IO], par: Parallel[IO]): IO[Unit] =
    for {
      logger <- Slf4jLogger.create[IO]
      _      <- new BlockedTokenMigration(conf, xa, logger).run
    } yield ()
}
