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

final class GenuineTokenMigration(
  conf: ProcessingConfig,
  xa: Transactor[IO],
  log: Logger[IO]
) {
  def run: IO[Unit] = updateSchema()

  def updateSchema(): IO[Unit] = {
    val txn = recreateGenuineTokensTable
    log.info("[updating DB schema]") >> txn.transact(xa)
  }

  def recreateGenuineTokensTable: ConnectionIO[Unit] =
    sql"""
         |create table if not exists genuine_tokens
         |(
         |    token_id        VARCHAR(64)   PRIMARY KEY,
         |    token_name      VARCHAR      NOT NULL,
         |    unique_name     BOOLEAN      NOT NULL,
         |    issuer          VARCHAR
         |)
         |""".stripMargin.update.run.void
}

object GenuineTokenMigration {

  def apply(
    conf: ProcessingConfig,
    xa: Transactor[IO]
  )(implicit timer: Timer[IO], par: Parallel[IO]): IO[Unit] =
    for {
      logger <- Slf4jLogger.create[IO]
      _      <- new GenuineTokenMigration(conf, xa, logger).run
    } yield ()
}
