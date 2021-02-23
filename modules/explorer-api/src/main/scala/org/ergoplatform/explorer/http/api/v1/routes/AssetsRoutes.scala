package org.ergoplatform.explorer.http.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v1.defs.AssetsEndpointDefs
import org.ergoplatform.explorer.http.api.v1.services.Assets
import org.ergoplatform.explorer.settings.RequestsSettings
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class AssetsRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](settings: RequestsSettings, service: Assets[F, fs2.Stream])(implicit opts: Http4sServerOptions[F]) {

  val defs = new AssetsEndpointDefs[F](settings)

  val routes: HttpRoutes[F] =
    listTokensR <+> getUnspentOutputsByAddressR

  private def getUnspentOutputsByAddressR: HttpRoutes[F] =
    defs.searchByTokenIdDef.toRoutes { case (q, paging) =>
      service.getAllLike(q, paging).adaptThrowable.value
    }

  private def listTokensR: HttpRoutes[F] =
    defs.listTokensDef.toRoutes { case (paging, ordering) =>
      service.getTokens(paging, ordering).adaptThrowable.value
    }
}

object AssetsRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    settings: RequestsSettings,
    service: Assets[F, fs2.Stream]
  )(implicit opts: Http4sServerOptions[F]): HttpRoutes[F] =
    new AssetsRoutes(settings, service).routes
}
