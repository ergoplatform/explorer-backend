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
import sttp.tapir.server.http4s._
import sttp.tapir.swagger.http4s.SwaggerHttp4s

final class DocsRoutes[F[_]: Concurrent: ContextShift: Timer](settings: RequestsSettings)(implicit
  opts: Http4sServerOptions[F]
) {

  import org.ergoplatform.explorer.http.api.v1.defs.DocsEndpointDefs._

  val routes: HttpRoutes[F] = openApiSpecR <+> swaggerApiSpecR

  private def allEndpoints =
    new TransactionsEndpointDefs(settings).endpoints ++
    new BoxesEndpointDefs(settings).endpoints ++
    new TokensEndpointDefs(settings).endpoints ++
    new EpochsEndpointDefs().endpoints ++
    new AddressesEndpointDefs(settings).endpoints

  private def tags =
    Tag("transactions", "Transactions methods".some) ::
    Tag("boxes", "Boxes methods".some) ::
    Tag("assets", "Assets methods".some) ::
    Tag("epochs", "Epochs methods".some) ::
    Tag("addresses", "Addresses methods".some) ::
    Nil

  private val docsAsYaml = allEndpoints
    .toOpenAPI("Ergo Explorer API v1", "1.0")
    .tags(tags)
    .toYaml

  private def openApiSpecR: HttpRoutes[F] =
    apiSpecDef.toRoutes { _ =>
      docsAsYaml
        .asRight[ApiErr]
        .pure[F]
    }

  private def swaggerApiSpecR: HttpRoutes[F] =
    new SwaggerHttp4s(docsAsYaml, contextPath = "api/v1/docs", yamlName = "openapi").routes
}

object DocsRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer](
    settings: RequestsSettings
  )(implicit opts: Http4sServerOptions[F]): HttpRoutes[F] =
    new DocsRoutes[F](settings).routes
}
