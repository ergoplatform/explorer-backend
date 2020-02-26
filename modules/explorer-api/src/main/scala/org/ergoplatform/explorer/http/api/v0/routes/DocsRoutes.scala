package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{ContextShift, Sync}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.v0.defs._
import org.http4s.HttpRoutes
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.server.http4s._

final class DocsRoutes[F[_]: Sync: ContextShift: Logger] {

  import org.ergoplatform.explorer.http.api.v0.defs.DocsEndpointDefs._

  val routes: HttpRoutes[F] = openApiSpecR

  private def allEndpoints =
    AddressesEndpointDefs.endpoints ++
    AssetsEndpointDefs.endpoints ++
    BlocksEndpointDefs.endpoints ++
    DexEndpointsDefs.endpoints ++
    TransactionsEndpointDefs.endpoints ++
    DocsEndpointDefs.endpoints

  private def openApiSpecR: HttpRoutes[F] =
    apiSpecDef.toRoutes { _ =>
      allEndpoints
        .toOpenAPI("Ergo Explorer API v0", "1.0")
        .toYaml
        .asRight[ApiErr]
        .pure[F]
    }
}

object DocsRoutes {

  def apply[F[_]: Sync: ContextShift]: F[HttpRoutes[F]] =
    Slf4jLogger.create.map { implicit logger =>
      new DocsRoutes[F].routes
    }
}
