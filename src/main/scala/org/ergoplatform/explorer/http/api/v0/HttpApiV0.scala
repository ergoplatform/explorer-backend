package org.ergoplatform.explorer.http.api.v0

import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import cats.{~>, Monad}
import org.ergoplatform.explorer.Err
import org.ergoplatform.explorer.algebra.Raise
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.http.api.v0.routes.BlocksRoutes
import org.ergoplatform.explorer.http.api.v0.services.BlockchainService
import org.ergoplatform.explorer.settings.HttpSettings
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Router, Server}
import org.http4s.syntax.kleisli._

object HttpApiV0 {

  def apply[
    F[_]: ConcurrentEffect: ContextShift: Timer,
    D[_]: LiftConnectionIO: Raise[*[_], Err.InconsistentDbData]: Monad
  ](settings: HttpSettings)(xa: D ~> F): Resource[F, Server[F]] =
    for {
      blockchainService <- Resource.liftF(BlockchainService(xa))
      http <- BlazeServerBuilder[F]
               .bindHttp(settings.port, settings.host)
               .withHttpApp(Router("/" -> BlocksRoutes(blockchainService)).orNotFound)
               .resource
    } yield http
}
