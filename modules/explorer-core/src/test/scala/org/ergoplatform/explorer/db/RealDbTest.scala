package org.ergoplatform.explorer.db

import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.ergoplatform.explorer.CatsInstances
import org.flywaydb.core.Flyway
import org.scalactic.source.Position
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, TestSuite}
import org.testcontainers.utility.DockerImageName

trait RealDbTest extends CatsInstances with BeforeAndAfter with BeforeAndAfterAll {
  self: TestSuite =>

  implicit lazy val xa: Transactor[IO] =
    Transactor.fromDriverManager[IO](
      psqlContainer.driverClassName,
      psqlContainer.jdbcUrl,
      psqlContainer.username,
      psqlContainer.password
    )

  private lazy val psqlContainer: PostgreSQLContainer =
    PostgreSQLContainer(DockerImageName.parse("postgres:11-alpine"), databaseName = "explorer", username = "ergo")

  private lazy val flyway = new Flyway()

  override def beforeAll(): Unit = {
    psqlContainer.container.start()
    flyway.setSqlMigrationSeparator("__")
    flyway.setLocations("classpath:db")
    flyway.setDataSource(psqlContainer.jdbcUrl, psqlContainer.username, psqlContainer.password)
    flyway.migrate()
  }

  override def afterAll(): Unit =
    psqlContainer.container.stop()

  override def after(fun: => Any)(implicit pos: Position): Unit =
    truncateAll()

  private def truncateAll(): Unit =
    sql"""
         |truncate box_registers;
         |truncate script_constants;
         |truncate node_inputs;
         |truncate node_outputs;
         |truncate node_headers, node_extensions, node_ad_proofs, blocks_info, node_transactions, node_assets, blocked_tokens, genuine_tokens;
         |""".stripMargin.update.run.transact(xa).unsafeRunSync()
}
