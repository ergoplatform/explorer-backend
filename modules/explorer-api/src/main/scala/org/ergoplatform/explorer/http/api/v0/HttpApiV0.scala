package org.ergoplatform.explorer.http.api.v0

import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import cats.{~>, Monad}
import cats.syntax.semigroupk._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.CRaise
import org.ergoplatform.explorer.Err.{RefinementFailed, RequestProcessingErr}
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.http.api.settings.HttpSettings
import org.ergoplatform.explorer.http.api.v0.routes.{
  AddressesRoutes,
  AssetsRoutes,
  BlocksRoutes,
  DexRoutes,
  DocsRoutes,
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
import org.ergoplatform.explorer.settings.ProtocolSettings
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Router, Server}
import org.http4s.syntax.kleisli._

object HttpApiV0 {

  /** Create an API v0 http server.
    */
  def apply[
    F[_]: ConcurrentEffect: ContextShift: Timer,
    D[_]: CRaise[*[_], RequestProcessingErr]: CRaise[*[_], RefinementFailed]: Monad: LiftConnectionIO
  ](settings: HttpSettings, protocolSettings: ProtocolSettings)(xa: D ~> F)(
    implicit e: ErgoAddressEncoder
  ): Resource[F, Server[F]] =
    for {
      blockChainService <- Resource.liftF(BlockChainService(xa))
      blockRoutes       <- Resource.liftF(BlocksRoutes(blockChainService))
      assetRoutes       <- Resource.liftF(AssetsRoutes(AssetsService(xa)))
      dexRoutes         <- Resource.liftF(DexRoutes(DexService(xa)))
      txRoutes          <- Resource.liftF(TransactionsRoutes(TransactionsService(xa)))
      addressRoutes <- Resource.liftF(
                        AddressesRoutes(AddressesService(xa), TransactionsService(xa))
                      )
      statsRoutes <- Resource.liftF(StatsRoutes(StatsService(protocolSettings)(xa)))
      docsRoutes  <- Resource.liftF(DocsRoutes[F])

      routes = blockRoutes <+> assetRoutes <+> dexRoutes <+> txRoutes <+> addressRoutes <+> statsRoutes <+> docsRoutes
      http <- BlazeServerBuilder[F]
               .bindHttp(settings.port, settings.host)
               .withHttpApp(Router("/" -> routes).orNotFound)
               .resource
    } yield http
}
