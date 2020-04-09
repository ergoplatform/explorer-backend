package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{ContextShift, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import cats.syntax.semigroupk._
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.models.Items
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v0.services.BlockChainService
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class BlocksRoutes[
  F[_]: Sync: ContextShift: AdaptThrowableEitherT[*[_], ApiErr]
](service: BlockChainService[F, fs2.Stream])(
  implicit opts: Http4sServerOptions[F]
) {

  import org.ergoplatform.explorer.http.api.v0.defs.BlocksEndpointDefs._

  val routes: HttpRoutes[F] =
    getBlocksR <+> getBlockSummaryByIdR <+> getBlockIdsAtHeightR

  private def getBlocksR: HttpRoutes[F] =
    getBlocksDef.toRoutes {
      case (paging, sorting) =>
        service.getBestHeight.flatMap { maxHeight =>
          service
            .getBlocks(paging, sorting)
            .compile
            .toList // API v0 format does not allow to use streaming
            .map(Items(_, maxHeight))
            .adaptThrowable
            .value
        }
    }

  private def getBlockSummaryByIdR: HttpRoutes[F] =
    getBlockSummaryByIdDef.toRoutes { id =>
      service
        .getBlockSummaryById(id)
        .flatMap(_.liftTo[F](ApiErr.notFound(s"Block with id: $id")))
        .adaptThrowable
        .value
    }

  private def getBlockIdsAtHeightR: HttpRoutes[F] =
    getBlockIdsAtHeightDef.toRoutes { height =>
      service
        .getBlockIdsAtHeight(height)
        .adaptThrowable
        .value
    }
}

object BlocksRoutes {

  def apply[F[_]: Sync: ContextShift](
    service: BlockChainService[F, fs2.Stream]
  )(implicit opts: Http4sServerOptions[F]): HttpRoutes[F] =
    new BlocksRoutes(service).routes
}
