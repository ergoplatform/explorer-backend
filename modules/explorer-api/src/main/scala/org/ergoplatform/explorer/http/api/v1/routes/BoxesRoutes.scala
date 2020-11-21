package org.ergoplatform.explorer.http.api.v1.routes

import cats.syntax.either._
import cats.syntax.semigroupk._
import cats.effect.{Concurrent, ContextShift, Timer}
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.v1.defs.BoxesEndpointDefs
import org.ergoplatform.explorer.http.api.v1.services.BoxesService
import org.ergoplatform.explorer.settings.ServiceSettings
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._
import tofu.syntax.monadic._
import io.circe.syntax._
import fs2.{Chunk, Stream}

final class BoxesRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](settings: ServiceSettings, service: BoxesService[F])(implicit opts: Http4sServerOptions[F]) {

  val defs = new BoxesEndpointDefs[F](settings)

  val routes: HttpRoutes[F] = streamUnspentOutputsByEpochsR <+> streamUnspentOutputsR

  private def streamUnspentOutputsR: HttpRoutes[F] =
    defs.streamUnspentOutputsDef.toRoutes { epochs =>
      service
        .getUnspentOutputs(epochs)
        .flatMap(entity => Stream.chunk(Chunk.array(entity.asJson.noSpaces.getBytes)))
        .pure
        .map(_.asRight[ApiErr])
    }

  private def streamUnspentOutputsByEpochsR: HttpRoutes[F] =
    defs.streamUnspentOutputsByEpochsDef.toRoutes { lastEpochs =>
      service
        .getUnspentOutputs(lastEpochs)
        .flatMap(entity => Stream.chunk(Chunk.array(entity.asJson.noSpaces.getBytes)))
        .pure
        .map(_.asRight[ApiErr])
    }
}

object BoxesRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    settings: ServiceSettings,
    service: BoxesService[F]
  )(implicit opts: Http4sServerOptions[F]): HttpRoutes[F] =
    new BoxesRoutes[F](settings, service).routes
}
