package org.ergoplatform.explorer.http.api.v0.routes

import cats.data.NonEmptyList
import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v0.services.AssetsService
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class AssetsRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](service: AssetsService[F, fs2.Stream])(
  implicit opts: Http4sServerOptions[F, F]
) {

  import org.ergoplatform.explorer.http.api.v0.defs.AssetsEndpointDefs._

  val routes: HttpRoutes[F] =
    getAllIssuingBoxesR <+> getIssuingBoxR

  private def interpreter = Http4sServerInterpreter(opts)

  private def getAllIssuingBoxesR: HttpRoutes[F] =
    interpreter.toRoutes(getAllIssuingBoxesDef) { paging =>
      service
        .getAllIssuingBoxes(paging)
        .adaptThrowable
        .value
    }

  private def getIssuingBoxR: HttpRoutes[F] =
    interpreter.toRoutes(getIssuingBoxDef) { tokenId =>
      service
        .getIssuingBoxes(NonEmptyList.one(tokenId))
        .compile
        .toList
        .adaptThrowable
        .value
    }
}

object AssetsRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    service: AssetsService[F, fs2.Stream]
  )(implicit opts: Http4sServerOptions[F, F]): HttpRoutes[F] =
    new AssetsRoutes(service).routes
}
