package org.ergoplatform.explorer.http.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.syntax.routes._
import org.ergoplatform.explorer.http.api.v1.defs.TokensEndpointDefs
import org.ergoplatform.explorer.http.api.v1.services.Tokens
import org.ergoplatform.explorer.settings.RequestsSettings
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class TokensRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](settings: RequestsSettings, service: Tokens[F, fs2.Stream])(implicit opts: Http4sServerOptions[F]) {

  val defs = new TokensEndpointDefs(settings)

  val routes: HttpRoutes[F] =
    listR <+> searchByIdR <+> getByIdR

  private def getByIdR: HttpRoutes[F] =
    defs.getByIdDef.toRoutes { id =>
      service
        .get(id)
        .adaptThrowable
        .orNotFound(s"Token with id: $id")
        .value
    }

  private def searchByIdR: HttpRoutes[F] =
    defs.searchByIdDef.toRoutes { case (q, paging) =>
      service.search(q, paging).adaptThrowable.value
    }

  private def listR: HttpRoutes[F] =
    defs.listDef.toRoutes { case (paging, ordering) =>
      service.getTokens(paging, ordering).adaptThrowable.value
    }
}

object TokensRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    settings: RequestsSettings,
    service: Tokens[F, fs2.Stream]
  )(implicit opts: Http4sServerOptions[F]): HttpRoutes[F] =
    new TokensRoutes(settings, service).routes
}
