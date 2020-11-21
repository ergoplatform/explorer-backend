package org.ergoplatform.explorer.http.api

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
import org.ergoplatform.explorer.http.api.v1.routes.RoutesV1Bundle
import org.ergoplatform.explorer.settings.{HttpSettings, ProtocolSettings, ServiceSettings, UtxCacheSettings}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware._
import org.http4s.server.{Router, Server}
import org.http4s.syntax.kleisli._
import sttp.tapir.server.http4s.Http4sServerOptions

import scala.concurrent.ExecutionContext

object HttpApi {

  /** Create an API v0 http server.
    */
  def apply[
    F[_]: ConcurrentEffect: ContextShift: Timer,
    D[_]: CRaise[*[_], RequestProcessingErr]: CRaise[*[_], RefinementFailed]: Monad: LiftConnectionIO
  ](
     httpSettings: HttpSettings,
     protocolSettings: ProtocolSettings,
     utxCacheSettings: UtxCacheSettings,
     serviceSettings: ServiceSettings,
     redis: RedisCommands[F, String, String]
  )(trans: D Trans F)(implicit
    ec: ExecutionContext,
    encoder: ErgoAddressEncoder,
    opts: Http4sServerOptions[F]
  ): Resource[F, Server[F]] =
    for {
      v0 <- Resource.liftF(RoutesV0Bundle(protocolSettings, utxCacheSettings, redis)(trans))
      v1 <- Resource.liftF(RoutesV1Bundle(serviceSettings)(trans))
      routes     = v0.routes <+> v1.routes
      corsRoutes = CORS(routes)
      http <- BlazeServerBuilder[F](ec)
                .bindHttp(httpSettings.port, httpSettings.host)
                .withHttpApp(Router("/" -> corsRoutes).orNotFound)
                .resource
    } yield http
}
