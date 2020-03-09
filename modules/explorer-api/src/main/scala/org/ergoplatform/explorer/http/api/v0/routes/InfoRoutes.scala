package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{ContextShift, Sync}
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v0.services.StatsService
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class InfoRoutes[F[_]: Sync: ContextShift](service: StatsService[F]) {

  import org.ergoplatform.explorer.http.api.v0.defs.InfoEndpointDefs._

  implicit private val adapt: AdaptThrowableEitherT[F, ApiErr] = implicitly

  val routes: HttpRoutes[F] = getCurrentStatsR

  private def getCurrentStatsR: HttpRoutes[F] =
    getBlockChainInfoDef.toRoutes(_ => service.getBlockChainInfo.adaptThrowable.value)
}

object InfoRoutes {

  def apply[F[_]: Sync: ContextShift](service: StatsService[F]): HttpRoutes[F] =
    new InfoRoutes(service).routes
}
