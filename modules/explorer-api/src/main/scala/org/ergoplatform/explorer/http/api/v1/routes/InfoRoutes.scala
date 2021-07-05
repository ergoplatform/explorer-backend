package org.ergoplatform.explorer.http.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.syntax.routes._
import org.ergoplatform.explorer.http.api.v1.defs.InfoEndpointsDefs
import org.ergoplatform.explorer.http.api.v1.services.NetworkInfos
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class InfoRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](infos: NetworkInfos[F])(implicit opts: Http4sServerOptions[F, F]) {

  val defs = new InfoEndpointsDefs()

  val routes: HttpRoutes[F] = getNetworkInfoR

  private def getNetworkInfoR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(defs.getNetworkInfo) { _ =>
      infos.getNetworkInfo.adaptThrowable
        .orNotFound(s"Latest network info")
        .value
    }
}

object InfoRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](infos: NetworkInfos[F])(implicit
    opts: Http4sServerOptions[F, F]
  ): HttpRoutes[F] =
    new InfoRoutes[F](infos).routes
}
