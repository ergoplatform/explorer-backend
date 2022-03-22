package org.ergoplatform.explorer.http.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.{streaming, ApiErr}
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.syntax.routes._
import org.ergoplatform.explorer.http.api.v1.defs.BlocksEndpointDefs
import org.ergoplatform.explorer.http.api.v1.services.Blocks
import org.ergoplatform.explorer.settings.RequestsSettings
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}

final class BlocksRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](settings: RequestsSettings, blocks: Blocks[F])(implicit opts: Http4sServerOptions[F, F]) {

  val defs = new BlocksEndpointDefs[F](settings)

  val routes: HttpRoutes[F] = streamBlocksR <+> getBlocksR <+> getBlockSummaryByIdR <+> getBlockHeadersR

  private def interpreter = Http4sServerInterpreter(opts)

  private def getBlocksR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getBlocksDef) { case (paging, sorting) =>
      blocks
        .getBlocks(paging, sorting)
        .adaptThrowable
        .value
    }

  private def streamBlocksR: HttpRoutes[F] =
    interpreter.toRoutes(defs.streamBlocksDef) { case (gix, limit) =>
      streaming.bytesStream(blocks.streamBlocks(gix, limit))
    }

  private def getBlockSummaryByIdR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getBlockSummaryByIdDef) { id =>
      blocks
        .getBlockSummaryById(id)
        .adaptThrowable
        .orNotFound(s"Block with id: $id")
        .value
    }

  private def getBlockHeadersR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getBlockHeadersDef) { case (paging, sorting)  =>
      blocks
        .getBlockHeaders(paging, sorting)
        .adaptThrowable
        .value
    }
}

object BlocksRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    settings: RequestsSettings,
    blocks: Blocks[F]
  )(implicit opts: Http4sServerOptions[F, F]): HttpRoutes[F] =
    new BlocksRoutes[F](settings, blocks).routes
}
