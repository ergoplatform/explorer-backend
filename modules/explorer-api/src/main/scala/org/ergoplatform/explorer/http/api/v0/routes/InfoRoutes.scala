package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{ContextShift, Sync}
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.explorer.http.api.v0.services.StatsService
import org.http4s.HttpRoutes
import org.ergoplatform.explorer.http.api.syntax.applicativeThrow._
import sttp.tapir.server.http4s._

final class InfoRoutes[F[_]: Sync: ContextShift: Logger](service: StatsService[F]) {

  import org.ergoplatform.explorer.http.api.v0.defs.InfoEndpointDefs._

  val routes: HttpRoutes[F] = getCurrentStatsR

  private def getCurrentStatsR: HttpRoutes[F] =
    getBlockChainInfoDef.toRoutes(_ => service.getBlockChainInfo.either)
}

object InfoRoutes {

  def apply[F[_]: Sync: ContextShift](service: StatsService[F]): F[HttpRoutes[F]] =
    Slf4jLogger.create.map { implicit logger =>
      new InfoRoutes(service).routes
    }
}
