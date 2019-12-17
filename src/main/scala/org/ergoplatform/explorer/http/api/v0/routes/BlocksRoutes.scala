package org.ergoplatform.explorer.http.api.v1.routes

import cats.data.{Kleisli, OptionT}
import cats.effect.{ContextShift, Sync}
import cats.syntax.flatMap._
import cats.syntax.option._
import org.ergoplatform.explorer.Err
import org.ergoplatform.explorer.services.BlockchainService
import org.ergoplatform.explorer.http.api.v1.syntax.applicativeThrow._
import org.http4s.{HttpRoutes, Request, Response}
import sttp.tapir.server.http4s._

final class BlocksRoutes[F[_]: Sync: ContextShift](service: BlockchainService[F]) {

  import org.ergoplatform.explorer.http.api.v1.defs.{BlocksEndpointDefs => Defs}

  val routes: Kleisli[OptionT[F, *], Request[F], Response[F]] =
    blockSummaryByIdR

  private def blockSummaryByIdR: HttpRoutes[F] =
    Defs.blockSummaryById.toRoutes { id =>
      service
        .getBlockSummaryById(id)
        .flatMap(_.liftTo[F](Err.NotFound(s"Block with id: $id")))
        .either
    }
}

object BlocksRoutes {

  def apply[F[_]: Sync: ContextShift](
    service: BlockchainService[F]
  ): Kleisli[OptionT[F, *], Request[F], Response[F]] =
    new BlocksRoutes(service).routes
}
