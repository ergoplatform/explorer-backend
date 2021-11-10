package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v0.modules.Search
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class SearchRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](search: Search[F])(implicit opts: Http4sServerOptions[F, F]) {

  import org.ergoplatform.explorer.http.api.v0.defs.SearchEndpointDefs._

  val routes: HttpRoutes[F] = searchR

  private def interpreter = Http4sServerInterpreter(opts)

  private def searchR: HttpRoutes[F] =
    interpreter.toRoutes(searchDef) { q =>
      search.search(q).adaptThrowable.value
    }
}

object SearchRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](search: Search[F])(implicit
    opts: Http4sServerOptions[F, F]
  ): HttpRoutes[F] =
    new SearchRoutes(search).routes
}
