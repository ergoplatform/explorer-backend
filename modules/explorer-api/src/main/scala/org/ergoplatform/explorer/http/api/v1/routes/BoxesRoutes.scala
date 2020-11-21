package org.ergoplatform.explorer.http.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.v1.defs.BoxesEndpointDefs
import org.ergoplatform.explorer.http.api.v1.services.BoxesService
import org.ergoplatform.explorer.http.api.{streaming, ApiErr}
import org.ergoplatform.explorer.settings.RequestsSettings
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._

final class BoxesRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](settings: RequestsSettings, service: BoxesService[F])(implicit opts: Http4sServerOptions[F]) {

  val defs = new BoxesEndpointDefs[F](settings)

  val routes: HttpRoutes[F] =
    streamUnspentOutputsByEpochsR <+>
    streamUnspentOutputsR <+>
    unspentOutputsByTokenIdR <+>
    outputsByTokenIdR

  private def streamUnspentOutputsR: HttpRoutes[F] =
    defs.streamUnspentOutputsDef.toRoutes { epochs =>
      streaming.bytesStream(service.getUnspentOutputs(epochs))
    }

  private def streamUnspentOutputsByEpochsR: HttpRoutes[F] =
    defs.streamUnspentOutputsByEpochsDef.toRoutes { lastEpochs =>
      streaming.bytesStream(service.getUnspentOutputs(lastEpochs))
    }

  private def outputsByTokenIdR: HttpRoutes[F] =
    defs.outputsByTokenIdDef.toRoutes {
      case (tokenId, paging) =>
        service.getOutputsByTokenId(tokenId, paging).adaptThrowable.value
    }

  private def unspentOutputsByTokenIdR: HttpRoutes[F] =
    defs.unspentOutputsByTokenIdDef.toRoutes {
      case (tokenId, paging) =>
        service.getUnspentOutputsByTokenId(tokenId, paging).adaptThrowable.value
    }
}

object BoxesRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    settings: RequestsSettings,
    service: BoxesService[F]
  )(implicit opts: Http4sServerOptions[F]): HttpRoutes[F] =
    new BoxesRoutes[F](settings, service).routes
}
