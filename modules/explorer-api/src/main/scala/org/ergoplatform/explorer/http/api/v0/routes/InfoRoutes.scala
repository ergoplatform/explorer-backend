package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{ContextShift, Sync}
import cats.syntax.functor._
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v0.services.StatsService
import org.ergoplatform.explorer.protocol.constants
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class InfoRoutes[
  F[_]: Sync: ContextShift: AdaptThrowableEitherT[*[_], ApiErr]
](service: StatsService[F])(implicit opts: Http4sServerOptions[F]) {

  import org.ergoplatform.explorer.http.api.v0.defs.InfoEndpointDefs._

  val routes: HttpRoutes[F] = getCurrentStatsR <+> getCurrentSupplyR

  private def getCurrentStatsR: HttpRoutes[F] =
    getBlockChainInfoDef.toRoutes(_ => service.getBlockChainInfo.adaptThrowable.value)

  private def getCurrentSupplyR: HttpRoutes[F] =
    getCurrentSupplyDef.toRoutes { _ =>
      service.getBlockChainInfo
        .map { info =>
          BigDecimal
            .apply(info.supply.toDouble / constants.CoinsInOneErgo)
            .setScale(constants.ErgoDecimalPlacesNum)
            .toString
        }
        .adaptThrowable
        .value
    }
}

object InfoRoutes {

  def apply[F[_]: Sync: ContextShift: Logger](service: StatsService[F])(
    implicit opts: Http4sServerOptions[F]
  ): HttpRoutes[F] =
    new InfoRoutes(service).routes
}
