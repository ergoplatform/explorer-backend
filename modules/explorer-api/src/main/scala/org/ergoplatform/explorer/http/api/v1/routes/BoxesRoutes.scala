package org.ergoplatform.explorer.http.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.v1.defs.BoxesEndpointDefs
import org.ergoplatform.explorer.http.api.v1.services.BoxesService
import org.ergoplatform.explorer.http.api.{streaming, ApiErr}
import org.ergoplatform.explorer.settings.ServiceSettings
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class BoxesRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](settings: ServiceSettings, service: BoxesService[F])(implicit opts: Http4sServerOptions[F]) {

  val defs = new BoxesEndpointDefs[F](settings)

  val routes: HttpRoutes[F] = streamUnspentOutputsByEpochsR <+> streamUnspentOutputsR

  private def streamUnspentOutputsR: HttpRoutes[F] =
    defs.streamUnspentOutputsDef.toRoutes { epochs =>
      streaming.bytesStream(service.getUnspentOutputs(epochs))
    }

  private def streamUnspentOutputsByEpochsR: HttpRoutes[F] =
    defs.streamUnspentOutputsByEpochsDef.toRoutes { lastEpochs =>
      streaming.bytesStream(service.getUnspentOutputs(lastEpochs))
    }
}

object BoxesRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    settings: ServiceSettings,
    service: BoxesService[F]
  )(implicit opts: Http4sServerOptions[F]): HttpRoutes[F] =
    new BoxesRoutes[F](settings, service).routes
}
