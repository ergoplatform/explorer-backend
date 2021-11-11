package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.functor._
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.syntax.routes._
import org.ergoplatform.explorer.http.api.v0.services.BlockChainService
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class BlocksRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](service: BlockChainService[F])(
  implicit opts: Http4sServerOptions[F, F]
) {

  import org.ergoplatform.explorer.http.api.v0.defs.BlocksEndpointDefs._

  val routes: HttpRoutes[F] =
    getBlocksR <+> getBlockSummaryByIdR <+> getBlockIdsAtHeightR

  private def interpreter = Http4sServerInterpreter(opts)

  private def getBlocksR: HttpRoutes[F] =
    interpreter.toRoutes(getBlocksDef) {
      case (paging, sorting) =>
        service
          .getBlocks(paging, sorting)
          .adaptThrowable
          .value
    }

  private def getBlockSummaryByIdR: HttpRoutes[F] =
    interpreter.toRoutes(getBlockSummaryByIdDef) { id =>
      service
        .getBlockSummaryById(id)
        .adaptThrowable
        .orNotFound(s"Block with id: $id")
        .value
    }

  private def getBlockIdsAtHeightR: HttpRoutes[F] =
    interpreter.toRoutes(getBlockIdsAtHeightDef) { height =>
      service
        .getBlockIdsAtHeight(height)
        .adaptThrowable
        .value
    }
}

object BlocksRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    service: BlockChainService[F]
  )(implicit opts: Http4sServerOptions[F, F]): HttpRoutes[F] =
    new BlocksRoutes(service).routes
}
