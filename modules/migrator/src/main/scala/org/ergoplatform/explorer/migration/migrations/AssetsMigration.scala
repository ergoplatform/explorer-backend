package org.ergoplatform.explorer.migration.migrations

import cats.effect.{IO, Timer}
import doobie.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.models.Token
import org.ergoplatform.explorer.db.repositories.TokenRepo
import org.ergoplatform.explorer.http.api.models.Paging
import org.ergoplatform.explorer.http.api.v0.models.TransactionInfo
import org.ergoplatform.explorer.http.api.v0.services.TransactionsService
import org.ergoplatform.explorer.migration.configs.ProcessingConfig
import org.ergoplatform.explorer.protocol.TokenPropsParser
import org.ergoplatform.explorer.{HexString, RegisterId, TokenId, TokenType}
import tofu.syntax.monadic._

final class AssetsMigration(
  conf: ProcessingConfig,
  tokenRepo: TokenRepo[ConnectionIO],
  txs: TransactionsService[IO],
  xa: Transactor[IO],
  log: Logger[IO]
)(implicit timer: Timer[IO]) {

  def run: IO[Unit] = migrateBatch(conf.offset, conf.batchSize)

  private def migrateBatch(offset: Int, limit: Int): IO[Unit] =
    log.info(s"Current offset is [$offset]") *> txs
      .getTxsSince(0, Paging(offset, limit))
      .flatMap {
        case Nil =>
          log.info(s"Migration complete")
        case txs =>
          val tokens = txs.flatMap(tokensFrom)
          log.info(s"Inserting [${tokens.size}] new tokens") >>
          tokenRepo.insertMany(tokens).transact(xa) >>
          IO.sleep(conf.interval) >>
          migrateBatch(offset + limit, limit)
      }

  private def tokensFrom(tx: TransactionInfo): Option[Token] = {
    val allowedTokenId = TokenId.fromStringUnsafe(tx.inputs.head.id.value)
    for {
      out  <- tx.outputs.find(_.assets.map(_.tokenId).contains(allowedTokenId))
      regs <- out.additionalRegisters.as[Map[RegisterId, HexString]].toOption
      props = TokenPropsParser.eip4Partial.parse(regs)
      asset <- out.assets.find(_.tokenId == allowedTokenId)
    } yield Token(
      asset.tokenId,
      out.id,
      asset.amount,
      props.map(_.name),
      props.map(_.description),
      props.map(_ => TokenType.Eip004),
      props.map(_.decimals)
    )
  }
}

object AssetsMigration {

  def apply(
    conf: ProcessingConfig,
    xa: Transactor[IO]
  )(implicit timer: Timer[IO], e: ErgoAddressEncoder): IO[Unit] =
    for {
      logger    <- Slf4jLogger.create[IO]
      tokenRepo <- TokenRepo[IO, ConnectionIO]
      txService <- TransactionsService[IO, ConnectionIO](Trans.fromDoobie(xa))
      _         <- new AssetsMigration(conf, tokenRepo, txService, xa, logger).run
    } yield ()
}
