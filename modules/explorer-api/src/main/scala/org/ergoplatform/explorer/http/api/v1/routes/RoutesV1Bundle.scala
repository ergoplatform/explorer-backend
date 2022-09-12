package org.ergoplatform.explorer.http.api.v1.routes

import cats.{Monad, Parallel}
import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import dev.profunktor.redis4cats.RedisCommands
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v1.services._
import org.ergoplatform.explorer.http.api.v1.shared._
import org.ergoplatform.explorer.settings.{RequestsSettings, ServiceSettings, UtxCacheSettings}
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.Http4sServerOptions
import tofu.Throws
import tofu.syntax.monadic._

import scala.concurrent.ExecutionContext

final case class RoutesV1Bundle[F[_]](routes: HttpRoutes[F])

object RoutesV1Bundle {

  def apply[
    F[_]: Concurrent: ContextShift: Timer: Parallel,
    D[_]: Monad: Throws: LiftConnectionIO: CompileStream
  ](
    serviceSettings: ServiceSettings,
    requestsSettings: RequestsSettings,
    utxCacheSettings: UtxCacheSettings,
    redis: Option[RedisCommands[F, String, String]]
  )(trans: D Trans F)(implicit
    ec: ExecutionContext,
    encoder: ErgoAddressEncoder,
    opts: Http4sServerOptions[F, F]
  ): F[RoutesV1Bundle[F]] =
    for {
      implicit0(log: Logger[F]) <- Slf4jLogger.create
      memprops                  <- MempoolProps(serviceSettings, utxCacheSettings, redis)(trans)
      mempool                   <- Mempool(serviceSettings, utxCacheSettings, redis, memprops)(trans)
      boxes                     <- Boxes(serviceSettings, memprops)(trans)
      addresses                 <- Addresses(serviceSettings, memprops)(trans)
      infos                     <- Networks(trans)
      tokens                    <- Tokens(trans)
      assets                    <- Assets(trans)
      epochs                    <- Epochs(trans)
      blocks                    <- Blocks(serviceSettings)(trans)
      transactions              <- Transactions(serviceSettings)(trans)
      infoRoutes      = StatsRoutes(infos)
      boxesRoutes     = BoxesRoutes(requestsSettings, boxes)
      epochsRoutes    = EpochsRoutes(epochs)
      blocksRoutes    = BlocksRoutes(requestsSettings, blocks)
      mempoolRoutes   = MempoolRoutes(mempool)
      tokensRoutes    = TokensRoutes(requestsSettings, tokens)
      assetsRoutes    = AssetsRoutes(requestsSettings, assets, tokens)
      txsRoutes       = TransactionsRoutes(requestsSettings, transactions)
      addressesRoutes = AddressesRoutes(requestsSettings, transactions, addresses)
      docs            = DocsRoutes(requestsSettings)
      routes =
        infoRoutes <+> txsRoutes <+> boxesRoutes <+> epochsRoutes <+> tokensRoutes <+> assetsRoutes <+> addressesRoutes <+> blocksRoutes <+> mempoolRoutes <+> docs
    } yield RoutesV1Bundle(routes)
}
