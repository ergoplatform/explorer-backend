package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{ContextShift, Sync}
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v0.services.StatsService
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class StatsRoutes[
  F[_]: Sync: ContextShift: AdaptThrowableEitherT[*[_], ApiErr]
](service: StatsService[F]) {

  import org.ergoplatform.explorer.http.api.v0.defs.StatsEndpointDefs._

  val routes: HttpRoutes[F] = getCurrentStatsR

  private def getCurrentStatsR: HttpRoutes[F] =
    getCurrentStatsDef.toRoutes { _ =>
      service.getCurrentStats.adaptThrowable.value
    }
}

object StatsRoutes {

  def apply[F[_]: Sync: ContextShift](service: StatsService[F]): HttpRoutes[F] =
    new StatsRoutes(service).routes
}
