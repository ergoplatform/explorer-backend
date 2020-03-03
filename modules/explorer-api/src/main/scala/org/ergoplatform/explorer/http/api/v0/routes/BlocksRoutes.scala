package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{ContextShift, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.models.Items
import org.ergoplatform.explorer.http.api.v0.services.BlockChainService
import org.ergoplatform.explorer.http.api.syntax.applicativeThrow._
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class BlocksRoutes[F[_]: Sync: ContextShift: Logger](
  service: BlockChainService[F, fs2.Stream]
) {

  import org.ergoplatform.explorer.http.api.v0.defs.BlocksEndpointDefs._

  val routes: HttpRoutes[F] =
    getBlocksR <+> getBlockSummaryByIdR

  private def getBlocksR: HttpRoutes[F] =
    getBlocksDef.toRoutes {
      case (paging, sorting) =>
        service.getBestHeight.flatMap { maxHeight =>
          service
            .getBlocks(paging, sorting)
            .compile
            .toList // API v0 format does not allow to use streaming
            .map(Items(_, maxHeight))
            .attemptApi
        }
    }

  private def getBlockSummaryByIdR: HttpRoutes[F] =
    getBlockSummaryByIdDef.toRoutes { id =>
      service
        .getBlockSummaryById(id)
        .flatMap(_.liftTo[F](ApiErr.NotFound(s"Block with id: $id")))
        .attemptApi
    }
}

object BlocksRoutes {

  def apply[F[_]: Sync: ContextShift](
    service: BlockChainService[F, fs2.Stream]
  ): F[HttpRoutes[F]] =
    Slf4jLogger.create.map { implicit logger =>
      new BlocksRoutes(service).routes
    }
}
