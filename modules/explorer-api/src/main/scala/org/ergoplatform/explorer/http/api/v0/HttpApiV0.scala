package org.ergoplatform.explorer.http.api.v0

import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import cats.{~>, Monad}
import org.ergoplatform.explorer.Err.RequestProcessingErr.{
  DexBuyOrderAttributesFailed,
  DexSellOrderAttributesFailed,
  InconsistentDbData
}
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.http.api.v0.routes.{
  AssetsRoutes,
  BlocksRoutes,
  DexRoutes
}
import org.ergoplatform.explorer.http.api.v0.services.{
  AssetsService,
  BlockChainService,
  DexService
}
import org.ergoplatform.explorer.settings.HttpSettings
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Router, Server}
import org.http4s.syntax.kleisli._
import tofu.Raise.ContravariantRaise

// TODO scaladoc
object HttpApiV0 {

  def apply[
    F[_]: ConcurrentEffect: ContextShift: Timer,
    D[_]: ContravariantRaise[*[_], InconsistentDbData]: ContravariantRaise[*[_], DexSellOrderAttributesFailed]: ContravariantRaise[
      *[_],
      DexBuyOrderAttributesFailed
    ]: Monad: LiftConnectionIO
  ](settings: HttpSettings)(xa: D ~> F): Resource[F, Server[F]] =
    for {
      blockChainService <- Resource.liftF(BlockChainService(xa))
      http <- BlazeServerBuilder[F]
               .bindHttp(settings.port, settings.host)
               .withHttpApp(Router("/" -> BlocksRoutes(blockChainService)).orNotFound)
               .withHttpApp(Router("/" -> AssetsRoutes(AssetsService(xa))).orNotFound)
               .withHttpApp(Router("/" -> DexRoutes(DexService(xa))).orNotFound)
               .resource
    } yield http
}
