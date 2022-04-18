package org.ergoplatform.explorer.migration.migrations

import cats.effect.{IO, Timer}
import cats.instances.list._
import cats.instances.try_._
import cats.syntax.traverse._
import doobie.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.syntax._
import org.ergoplatform.explorer.db.doobieInstances._
import org.ergoplatform.explorer.db.models.{BoxRegister, Output}
import org.ergoplatform.explorer.db.repositories.BoxRegisterRepo
import org.ergoplatform.explorer.migration.configs.ProcessingConfig
import org.ergoplatform.explorer.protocol.RegistersParser
import org.ergoplatform.explorer.protocol.models.{ExpandedRegister, RegisterValue}
import org.ergoplatform.explorer.{HexString, RegisterId}
import tofu.syntax.monadic._

import scala.util.Try

final class RegistersMigration(
  conf: ProcessingConfig,
  registers: BoxRegisterRepo[ConnectionIO],
  xa: Transactor[IO],
  log: Logger[IO]
)(implicit timer: Timer[IO]) {

  def run: IO[Unit] = migrateBatch(conf.offset, conf.batchSize)

  def migrateBatch(offset: Int, limit: Int): IO[Unit] =
    log.info(s"Current offset is [$offset]") *> outputsBatch(offset, limit)
      .transact(xa)
      .flatMap {
        _.traverse { out =>
          expandRegisters(out)
            .flatMap {
              case (_, Nil) =>
                IO.unit
              case (out, regs) =>
                val txn = updateOutput(out) >> registers.insertMany(regs)
                txn.transact(xa)
            }
            .handleErrorWith(e => log.error(e)("Error while migrating registers"))
        }.flatMap {
          case Nil => IO.unit
          case xs =>
            log.info(s"[${xs.size}] boxes processed") *>
              IO.sleep(conf.interval) >>
              migrateBatch(offset + limit, limit)
        }
      }

  def expandRegisters(out: Output): IO[(Output, List[BoxRegister])] =
    out.additionalRegisters
      .as[Map[RegisterId, HexString]]
      .fold(e => IO.raiseError(new Exception(s"Cannot deserialize registers in: $out, $e")), IO.pure)
      .map { rawRegisters =>
        val registers = for {
          (id, rawValue)                  <- rawRegisters.toList
          RegisterValue(valueType, value) <- RegistersParser[Try].parseAny(rawValue).toOption
        } yield BoxRegister(id, out.boxId, valueType, rawValue, value)
        val registersJson = registers
          .map { case BoxRegister(id, _, valueType, rawValue, decodedValue) =>
            id.entryName -> ExpandedRegister(rawValue, Some(valueType), Some(decodedValue))
          }
          .toMap
          .asJson
        val updatedOutput = out.copy(additionalRegisters = registersJson)
        updatedOutput -> registers
      }

  def outputsBatch(offset: Int, limit: Int): ConnectionIO[List[Output]] =
    sql"""
         |select
         |  o.box_id,
         |  o.tx_id,
         |  o.header_id,
         |  o.value,
         |  o.creation_height,
         |  o.index,
         |  o.ergo_tree,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain
         |from node_outputs o
         |left join box_registers r on o.box_id = r.box_id
         |where r.id is null and o.additional_registers::text != '{}'
         |order by o.creation_height asc
         |offset $offset limit $limit
         """.stripMargin.query[Output].to[List]

  def updateOutput(output: Output): ConnectionIO[Unit] =
    sql"""
         |update node_outputs set additional_registers = ${output.additionalRegisters}
         |where box_id = ${output.boxId}
         """.stripMargin.update.run.void
}

object RegistersMigration {

  def apply(
    conf: ProcessingConfig,
    xa: Transactor[IO]
  )(implicit timer: Timer[IO]): IO[Unit] =
    for {
      logger <- Slf4jLogger.create[IO]
      repo   <- BoxRegisterRepo[IO, ConnectionIO]
      _      <- new RegistersMigration(conf, repo, xa, logger).run
    } yield ()
}
