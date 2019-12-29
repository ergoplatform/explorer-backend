package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{ContextShift, Sync}
import cats.syntax.flatMap._
import cats.syntax.option._
import org.ergoplatform.explorer.Err.ApiErr
import org.ergoplatform.explorer.http.api.v0.services.BlockchainService
import org.ergoplatform.explorer.http.api.v0.syntax.applicativeThrow._
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class BlocksRoutes[F[_]: Sync: ContextShift](service: BlockchainService[F, fs2.Stream]) {

  import org.ergoplatform.explorer.http.api.v0.defs.BlocksEndpointDefs._

  val routes: HttpRoutes[F] =
    blockSummaryByIdR

  private def blockSummaryByIdR: HttpRoutes[F] =
    getBlockSummaryByIdDef.toRoutes { id =>
      service
        .getBlockSummaryById(id)
        .flatMap(_.liftTo[F](ApiErr.NotFound(s"Block with id: $id")))
        .either
    }
}

object BlocksRoutes {

  def apply[F[_]: Sync: ContextShift](
    service: BlockchainService[F, fs2.Stream]
  ): HttpRoutes[F] =
    new BlocksRoutes(service).routes
}
