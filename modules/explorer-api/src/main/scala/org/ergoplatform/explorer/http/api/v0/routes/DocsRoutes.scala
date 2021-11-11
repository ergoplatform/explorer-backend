package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.option._
import cats.syntax.semigroupk._
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.v0.defs._
import org.http4s.HttpRoutes
import sttp.tapir.apispec.Tag
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.redoc.http4s.RedocHttp4s
import sttp.tapir.server.http4s._

final class DocsRoutes[F[_]: Concurrent: ContextShift: Timer](implicit opts: Http4sServerOptions[F, F]) {

  import org.ergoplatform.explorer.http.api.v0.defs.DocsEndpointDefs._

  val routes: HttpRoutes[F] = openApiSpecR <+> redocApiSpecR

  private def interpreter = Http4sServerInterpreter(opts)

  private def allEndpoints =
    AddressesEndpointDefs.endpoints ++
    AssetsEndpointDefs.endpoints ++
    BlocksEndpointDefs.endpoints ++
    DexEndpointsDefs.endpoints ++
    TransactionsEndpointDefs.endpoints ++
    BoxesEndpointDefs.endpoints ++
    SearchEndpointDefs.endpoints ++
    InfoEndpointDefs.endpoints ++
    StatsEndpointDefs.endpoints ++
    DocsEndpointDefs.endpoints

  private def tags =
    Tag("info", "General network info".some) ::
    Tag("transactions", "Transactions info".some) ::
    Tag("addresses", "Addresses info, balances etc.".some) ::
    Tag("assets", "Assets info".some) ::
    Tag("blocks", "Blocks info".some) ::
    Tag("charts", "Charts".some) ::
    Tag("stats", "Network statistics".some) ::
    Tag("dex", "Dex orders info".some) ::
    Tag("search", "Search any entity".some) ::
    Tag("docs", "API documentation".some) ::
    Nil

  private val docsAsYaml =
    OpenAPIDocsInterpreter()
      .toOpenAPI(allEndpoints, "Ergo Explorer API v0", "1.0")
      .tags(tags)
      .toYaml

  private def openApiSpecR: HttpRoutes[F] =
    interpreter.toRoutes(apiSpecDef) { _ =>
      docsAsYaml
        .asRight[ApiErr]
        .pure[F]
    }

  private def redocApiSpecR: HttpRoutes[F] =
    new RedocHttp4s(
      "Redoc",
      docsAsYaml,
      "openapi",
      contextPath = "api" :: "v0" :: "docs" :: Nil
    ).routes
}

object DocsRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer](implicit opts: Http4sServerOptions[F, F]): HttpRoutes[F] =
    new DocsRoutes[F].routes
}
