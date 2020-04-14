package org.ergoplatform.explorer.http.api.v0

import cats.Monad
import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import cats.syntax.semigroupk._
import dev.profunktor.redis4cats.algebra.RedisCommands
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.CRaise
import org.ergoplatform.explorer.Err.{RefinementFailed, RequestProcessingErr}
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.http.api.v0.routes._
import org.ergoplatform.explorer.http.api.v0.services._
import org.ergoplatform.explorer.settings.{HttpSettings, ProtocolSettings, UtxCacheSettings}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware._
import org.http4s.server.{Router, Server}
import org.http4s.syntax.kleisli._
import sttp.tapir.server.http4s.Http4sServerOptions

object HttpApiV0 {

  /** Create an API v0 http server.
    */
  def apply[
    F[_]: ConcurrentEffect: ContextShift: Timer,
    D[_]: CRaise[*[_], RequestProcessingErr]: CRaise[*[_], RefinementFailed]: Monad: LiftConnectionIO
  ](
    settings: HttpSettings,
    protocolSettings: ProtocolSettings,
    utxCacheSettings: UtxCacheSettings,
    redis: RedisCommands[F, String, String]
  )(trans: D Trans F)(
    implicit
    encoder: ErgoAddressEncoder,
    opts: Http4sServerOptions[F]
  ): Resource[F, Server[F]] =
    for {
      implicit0(log: Logger[F]) <- Resource.liftF(Slf4jLogger.create)
      blockChainService         <- Resource.liftF(BlockChainService(trans))
      assetsService             <- Resource.liftF(AssetsService(trans))
      dexService                <- Resource.liftF(DexService(trans))
      addressesService          <- Resource.liftF(AddressesService(trans))
      statsService              <- Resource.liftF(StatsService(protocolSettings)(trans))
      boxesService              <- Resource.liftF(BoxesService(trans))
      txsService                <- Resource.liftF(TransactionsService(utxCacheSettings, redis)(trans))
      blockRoutes   = BlocksRoutes(blockChainService)
      assetRoutes   = AssetsRoutes(assetsService)
      dexRoutes     = DexRoutes(dexService)
      txRoutes      = TransactionsRoutes(txsService)
      addressRoutes = AddressesRoutes(addressesService, txsService)
      infoRoutes    = InfoRoutes(statsService)
      statsRoutes   = StatsRoutes(statsService)
      chartsRoutes  = ChartsRoutes(statsService)
      docsRoutes    = DocsRoutes[F]
      searchRoutes  = SearchRoutes(blockChainService, txsService, addressesService)
      boxesRoutes   = BoxesRoutes(boxesService)

      routes = infoRoutes <+> blockRoutes <+> assetRoutes <+> dexRoutes <+> txRoutes <+>
      addressRoutes <+> statsRoutes <+> docsRoutes <+> searchRoutes <+> boxesRoutes <+> chartsRoutes
      corsRoutes = CORS(routes)
      http <- BlazeServerBuilder[F]
               .bindHttp(settings.port, settings.host)
               .withHttpApp(Router("/" -> corsRoutes).orNotFound)
               .resource
    } yield http
}
