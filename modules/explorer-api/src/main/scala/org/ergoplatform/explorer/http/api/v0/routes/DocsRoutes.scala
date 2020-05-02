package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{ContextShift, Sync}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.option._
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.v0.defs._
import org.http4s.HttpRoutes
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.Tag
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.server.http4s._

final class DocsRoutes[F[_]: Sync: ContextShift](implicit opts: Http4sServerOptions[F]) {

  import org.ergoplatform.explorer.http.api.v0.defs.DocsEndpointDefs._

  val routes: HttpRoutes[F] = openApiSpecR

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

  private def openApiSpecR: HttpRoutes[F] =
    apiSpecDef.toRoutes { _ =>
      allEndpoints
        .toOpenAPI("Ergo Explorer API v0", "1.0")
        .tags(tags)
        .toYaml
        .asRight[ApiErr]
        .pure[F]
    }
}

object DocsRoutes {

  def apply[F[_]: Sync: ContextShift](implicit opts: Http4sServerOptions[F]): HttpRoutes[F] =
    new DocsRoutes[F].routes
}
