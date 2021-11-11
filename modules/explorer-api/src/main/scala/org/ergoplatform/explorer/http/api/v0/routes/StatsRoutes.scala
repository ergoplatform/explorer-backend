package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{Concurrent, ContextShift, Sync, Timer}
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v0.services.StatsService
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class StatsRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](service: StatsService[F])(implicit opts: Http4sServerOptions[F, F]) {

  import org.ergoplatform.explorer.http.api.v0.defs.StatsEndpointDefs._

  val routes: HttpRoutes[F] = getCurrentStatsR

  private def interpreter = Http4sServerInterpreter(opts)

  private def getCurrentStatsR: HttpRoutes[F] =
    interpreter.toRoutes(getCurrentStatsDef) { _ =>
      service.getCurrentStats.adaptThrowable.value
    }
}

object StatsRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](service: StatsService[F])(
    implicit opts: Http4sServerOptions[F, F]
  ): HttpRoutes[F] =
    new StatsRoutes(service).routes
}
