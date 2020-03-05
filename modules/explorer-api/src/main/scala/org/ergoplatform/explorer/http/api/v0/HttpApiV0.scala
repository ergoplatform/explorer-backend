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
import org.ergoplatform.explorer.http.api.settings.HttpSettings
import org.ergoplatform.explorer.http.api.v0.routes.{
  AddressesRoutes,
  AssetsRoutes,
  BlocksRoutes,
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
      blockRoutes       <- Resource.liftF(BlocksRoutes(blockChainService))
      assetRoutes       <- Resource.liftF(AssetsRoutes(AssetsService(trans)))
      dexRoutes         <- Resource.liftF(DexRoutes(DexService(trans)))
      txRoutes          <- Resource.liftF(TransactionsRoutes(txsService))
      addressRoutes     <- Resource.liftF(AddressesRoutes(AddressesService(trans), txsService))
      statsService = StatsService(protocolSettings)(trans)
      infoRoutes  <- Resource.liftF(InfoRoutes(statsService))
      statsRoutes <- Resource.liftF(StatsRoutes(statsService))
      docsRoutes  <- Resource.liftF(DocsRoutes[F])
      searchRoutes <- Resource.liftF(
                       SearchRoutes(blockChainService, txsService, AddressesService(trans))
                     )

      routes = infoRoutes <+> blockRoutes <+> assetRoutes <+> dexRoutes <+> txRoutes <+>
      addressRoutes <+> statsRoutes <+> docsRoutes <+> searchRoutes
      http <- BlazeServerBuilder[F]
               .bindHttp(settings.port, settings.host)
               .withHttpApp(Router("/" -> routes).orNotFound)
               .resource
    } yield http
}
