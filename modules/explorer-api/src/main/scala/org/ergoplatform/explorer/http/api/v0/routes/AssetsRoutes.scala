package org.ergoplatform.explorer.http.api.v0.routes

import cats.data.NonEmptyList
import cats.effect.{ContextShift, Sync}
import cats.syntax.semigroupk._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.models.Items
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v0.services.AssetsService
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class AssetsRoutes[
  F[_]: Sync: ContextShift: AdaptThrowableEitherT[*[_], ApiErr]
](service: AssetsService[F, fs2.Stream])(
  implicit opts: Http4sServerOptions[F]
) {

  import org.ergoplatform.explorer.http.api.v0.defs.AssetsEndpointDefs._

  val routes: HttpRoutes[F] =
    getAllIssuingBoxesR <+> getIssuingBoxR

  private def getAllIssuingBoxesR: HttpRoutes[F] =
    getAllIssuingBoxesDef.toRoutes { paging =>
      service
        .getAllIssuingBoxes(paging)
        .adaptThrowable
        .value
    }

  private def getIssuingBoxR: HttpRoutes[F] =
    getIssuingBoxDef.toRoutes { tokenId =>
      service
        .getIssuingBoxes(NonEmptyList.one(tokenId))
        .compile
        .toList
        .adaptThrowable
        .value
    }
}

object AssetsRoutes {

  def apply[F[_]: Sync: ContextShift: Logger](
    service: AssetsService[F, fs2.Stream]
  )(implicit opts: Http4sServerOptions[F]): HttpRoutes[F] =
    new AssetsRoutes(service).routes
}
