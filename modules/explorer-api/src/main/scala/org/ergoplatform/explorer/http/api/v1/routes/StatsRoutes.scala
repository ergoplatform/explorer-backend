package org.ergoplatform.explorer.http.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.syntax.routes._
import org.ergoplatform.explorer.http.api.v1.defs.StatsEndpointsDefs
import org.ergoplatform.explorer.http.api.v1.services.Networks
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class StatsRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](infos: Networks[F])(implicit opts: Http4sServerOptions[F, F]) {

  val defs = new StatsEndpointsDefs()

  val routes: HttpRoutes[F] = getNetworkInfoR <+> getNetworkStateR <+> getNetworkStatsR

  private def interpreter = Http4sServerInterpreter(opts)

  private def getNetworkInfoR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getNetworkInfo) { _ =>
      infos.getState.adaptThrowable
        .orNotFound(s"Latest network info")
        .value
    }

  private def getNetworkStateR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getNetworkState) { _ =>
      infos.getState.adaptThrowable
        .orNotFound(s"Latest network state")
        .value
    }

  private def getNetworkStatsR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getNetworkStats) { _ =>
      infos.getStats.adaptThrowable.value
    }
}

object StatsRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](infos: Networks[F])(implicit
    opts: Http4sServerOptions[F, F]
  ): HttpRoutes[F] =
    new StatsRoutes[F](infos).routes
}
