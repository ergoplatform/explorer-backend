package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{ContextShift, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import cats.syntax.semigroupk._
import org.ergoplatform.explorer.Err.ApiErr
import org.ergoplatform.explorer.http.api.models.Items
import org.ergoplatform.explorer.http.api.v0.services.BlockChainService
import org.ergoplatform.explorer.http.api.syntax.applicativeThrow._
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class BlocksRoutes[F[_]: Sync: ContextShift](
  service: BlockChainService[F, fs2.Stream]
) {

  import org.ergoplatform.explorer.http.api.v0.defs.BlocksEndpointDefs._

  val routes: HttpRoutes[F] =
    getBlocksR <+> getBlockSummaryByIdR

  private def getBlocksR: HttpRoutes[F] =
    getBlocksDef.toRoutes { paging =>
      service.getBestHeight.flatMap { maxHeight =>
        service
          .getBlocks(paging)
          .compile
          .toList // API v0 format does not allow to use streaming
          .map(Items(_, maxHeight))
          .either
      }
    }

  private def getBlockSummaryByIdR: HttpRoutes[F] =
    getBlockSummaryByIdDef.toRoutes { id =>
      service
        .getBlockSummaryById(id)
        .flatMap(_.liftTo[F](ApiErr.NotFound(s"Block with id: $id")))
        .either
    }
}

object BlocksRoutes {

  def apply[F[_]: Sync: ContextShift](
    service: BlockChainService[F, fs2.Stream]
  ): HttpRoutes[F] =
    new BlocksRoutes(service).routes
}
