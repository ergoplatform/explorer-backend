package org.ergoplatform.explorer.http.api.v0

import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import cats.Monad
import cats.syntax.semigroupk._
import dev.profunktor.redis4cats.algebra.RedisCommands
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.CRaise
import org.ergoplatform.explorer.Err.{RefinementFailed, RequestProcessingErr}
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.settings.HttpSettings
import org.ergoplatform.explorer.http.api.v0.routes.{
  AddressesRoutes,
  AssetsRoutes,
  BlocksRoutes,
  BoxesRoutes,
  DexRoutes,
  DocsRoutes,
  InfoRoutes,
  SearchRoutes,
  StatsRoutes,
  TransactionsRoutes
}
import org.ergoplatform.explorer.http.api.v0.services.{
  AddressesService,
  AssetsService,
  BlockChainService,
  BoxesService,
  DexService,
  StatsService,
  TransactionsService
}
import org.ergoplatform.explorer.settings.{ProtocolSettings, UtxCacheSettings}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Router, Server}
import org.http4s.syntax.kleisli._

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
    implicit e: ErgoAddressEncoder
  ): Resource[F, Server[F]] =
    for {
      blockChainService <- Resource.liftF(BlockChainService(trans))
      txsService        <- Resource.liftF(TransactionsService(utxCacheSettings, redis)(trans))
      blockRoutes   = BlocksRoutes(blockChainService)
      assetRoutes   = AssetsRoutes(AssetsService(trans))
      dexRoutes     = DexRoutes(DexService(trans))
      txRoutes      = TransactionsRoutes(txsService)
      addressRoutes = AddressesRoutes(AddressesService(trans), txsService)
      statsService  = StatsService(protocolSettings)(trans)
      infoRoutes    = InfoRoutes(statsService)
      statsRoutes   = StatsRoutes(statsService)
      docsRoutes    = DocsRoutes[F]
      searchRoutes  = SearchRoutes(blockChainService, txsService, AddressesService(trans))
      boxesRoutes   = BoxesRoutes(BoxesService(trans))

      routes = infoRoutes <+> blockRoutes <+> assetRoutes <+> dexRoutes <+> txRoutes <+>
      addressRoutes <+> statsRoutes <+> docsRoutes <+> searchRoutes <+> boxesRoutes
      http <- BlazeServerBuilder[F]
               .bindHttp(settings.port, settings.host)
               .withHttpApp(Router("/" -> routes).orNotFound)
               .resource
    } yield http
}
