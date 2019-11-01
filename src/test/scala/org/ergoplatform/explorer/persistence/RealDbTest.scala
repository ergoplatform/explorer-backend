package org.ergoplatform.explorer.persistence

import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.util.transactor.Transactor
import doobie.implicits._
import org.ergoplatform.explorer.CatsInstances
import org.flywaydb.core.Flyway
import org.scalactic.source.Position
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, TestSuite}

trait RealDbTest
  extends CatsInstances
    with BeforeAndAfter
    with BeforeAndAfterAll { self: TestSuite =>

  val container: PostgreSQLContainer = {
    val c = PostgreSQLContainer("postgres:latest")
    c.container.withUsername("ergo").withDatabaseName("explorer")
    c
  }

  lazy val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    container.driverClassName,
    container.jdbcUrl,
    container.username,
    container.password
  )

  private lazy val flyway = new Flyway()

  override def beforeAll(): Unit = {
    container.container.start()
    flyway.setSqlMigrationSeparator("__")
    flyway.setLocations("classpath:db")
    flyway.setDataSource(container.jdbcUrl, container.username, container.password)
    flyway.migrate()
  }

  override def afterAll(): Unit =
    container.container.stop()

  override def after(fun: => Any)(implicit pos: Position): Unit =
    truncateAll()

  private def truncateAll(): Unit =
    sql"""
         |truncate node_inputs;
         |truncate node_outputs;
         |truncate node_headers, node_extensions, node_ad_proofs, blocks_info, node_transactions;
         |""".stripMargin.update.run.transact(xa).unsafeRunSync()
}
