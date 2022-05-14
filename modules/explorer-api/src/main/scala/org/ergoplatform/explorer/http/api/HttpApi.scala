package org.ergoplatform.explorer.http.api

import cats.Monad
import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import cats.syntax.semigroupk._
import dev.profunktor.redis4cats.algebra.RedisCommands
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v0.routes.RoutesV0Bundle
import org.ergoplatform.explorer.http.api.v1.routes.RoutesV1Bundle
import org.ergoplatform.explorer.settings.ApiSettings
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.middleware._
import org.http4s.server.{Router, Server}
import sttp.tapir.server.http4s.Http4sServerOptions
import tofu.Throws

import scala.concurrent.ExecutionContext

object HttpApi {

  /** Create an API v0 http server.
    */
  def apply[
    F[_]: ConcurrentEffect: ContextShift: Timer,
    D[_]: Monad: Throws: LiftConnectionIO: CompileStream
  ](
    settings: ApiSettings,
    redis: Option[RedisCommands[F, String, String]]
  )(trans: D Trans F)(implicit
    ec: ExecutionContext,
    encoder: ErgoAddressEncoder,
    opts: Http4sServerOptions[F, F]
  ): Resource[F, Server] =
    for {
      v0 <- Resource.eval(RoutesV0Bundle(settings.protocol, settings.utxCache, redis)(trans))
      v1 <- Resource.eval(RoutesV1Bundle(settings.service, settings.requests, settings.utxCache, redis)(trans))
      routes     = v0.routes <+> v1.routes
      corsRoutes = CORS.policy.withAllowOriginAll(routes)
      http <- BlazeServerBuilder[F](ec)
                .bindHttp(settings.http.port, settings.http.host)
                .withHttpApp(Router("/" -> corsRoutes).orNotFound)
                .resource
    } yield http
}
