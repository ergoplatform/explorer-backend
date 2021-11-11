package org.ergoplatform.explorer.http.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.option._
import cats.syntax.semigroupk._
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.v1.defs._
import org.ergoplatform.explorer.settings.RequestsSettings
import org.http4s.HttpRoutes
import sttp.tapir.apispec.Tag
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.redoc.http4s.RedocHttp4s
import sttp.tapir.server.http4s._

final class DocsRoutes[F[_]: Concurrent: ContextShift: Timer](settings: RequestsSettings)(implicit
  opts: Http4sServerOptions[F, F]
) {

  import org.ergoplatform.explorer.http.api.v1.defs.DocsEndpointDefs._

  val routes: HttpRoutes[F] = openApiSpecR <+> redocApiSpecR

  private def interpreter = Http4sServerInterpreter(opts)

  private def allEndpoints =
    new TransactionsEndpointDefs(settings).endpoints ++
    new BoxesEndpointDefs(settings).endpoints ++
    new TokensEndpointDefs(settings).endpoints ++
    new AssetsEndpointDefs(settings).endpoints ++
    new EpochsEndpointDefs().endpoints ++
    new AddressesEndpointDefs(settings).endpoints ++
    new BlocksEndpointDefs(settings).endpoints ++
    new MempoolEndpointDefs().endpoints ++
    new StatsEndpointsDefs().endpoints

  private def tags =
    Tag("transactions", "Transactions methods".some) ::
    Tag("boxes", "Boxes methods".some) ::
    Tag("assets", "Assets methods".some) ::
    Tag("epochs", "Epochs methods".some) ::
    Tag("addresses", "Addresses methods".some) ::
    Tag("blocks", "Blocks methods".some) ::
    Nil

  private val docsAsYaml =
    OpenAPIDocsInterpreter()
      .toOpenAPI(allEndpoints, "Ergo Explorer API v1", "1.0")
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
      contextPath = "api" :: "v1" :: "docs" :: Nil
    ).routes
}

object DocsRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer](
    settings: RequestsSettings
  )(implicit opts: Http4sServerOptions[F, F]): HttpRoutes[F] =
    new DocsRoutes[F](settings).routes
}
