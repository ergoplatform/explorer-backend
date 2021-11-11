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
](settings: RequestsSettings, tokens: Tokens[F])(implicit opts: Http4sServerOptions[F, F]) {

  val defs = new TokensEndpointDefs(settings)

  val routes: HttpRoutes[F] =
    listR <+> searchR <+> getBySymbolR <+> getByIdR

  private def interpreter = Http4sServerInterpreter(opts)

  private def getByIdR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getByIdDef) { id =>
      tokens
        .get(id)
        .adaptThrowable
        .orNotFound(s"Token with id: $id")
        .value
    }

  private def getBySymbolR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getBySymbolDef) { sym =>
      tokens
        .getBySymbol(sym)
        .adaptThrowable
        .value
    }

  private def searchR: HttpRoutes[F] =
    interpreter.toRoutes(defs.searchDef) { case (q, paging) =>
      tokens.search(q, paging).adaptThrowable.value
    }

  private def listR: HttpRoutes[F] =
    interpreter.toRoutes(defs.listDef) { case (paging, ordering, hideNfts) =>
      tokens.getAll(paging, ordering, hideNfts).adaptThrowable.value
    }
}

object TokensRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    settings: RequestsSettings,
    tokens: Tokens[F]
  )(implicit opts: Http4sServerOptions[F, F]): HttpRoutes[F] =
    new TokensRoutes(settings, tokens).routes
}
