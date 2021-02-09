package org.ergoplatform.explorer.http.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.syntax.routes._
import org.ergoplatform.explorer.http.api.v1.defs.BoxesEndpointDefs
import org.ergoplatform.explorer.http.api.v1.services.BoxesService
import org.ergoplatform.explorer.http.api.{streaming, ApiErr}
import org.ergoplatform.explorer.settings.RequestsSettings
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class BoxesRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](settings: RequestsSettings, service: BoxesService[F])(implicit opts: Http4sServerOptions[F]) {

  val defs = new BoxesEndpointDefs[F](settings)

  val routes: HttpRoutes[F] =
    streamUnspentOutputsByEpochsR <+>
    streamUnspentOutputsR <+>
    streamOutputsByErgoTreeTemplateHashR <+>
    streamUnspentOutputsByErgoTreeTemplateHashR <+>
    unspentOutputsByTokenIdR <+>
    outputsByTokenIdR <+>
    getOutputByIdR <+>
    getOutputsByErgoTreeR <+>
    getUnspentOutputsByErgoTreeR <+>
    getOutputsByErgoTreeTemplateHashR <+>
    getUnspentOutputsByErgoTreeTemplateHashR <+>
    getOutputsByAddressR <+>
    getUnspentOutputsByAddressR

  private def streamUnspentOutputsR: HttpRoutes[F] =
    defs.streamUnspentOutputsDef.toRoutes { epochs =>
      streaming.bytesStream(service.streamUnspentOutputs(epochs))
    }

  private def streamUnspentOutputsByEpochsR: HttpRoutes[F] =
    defs.streamUnspentOutputsByEpochsDef.toRoutes { lastEpochs =>
      streaming.bytesStream(service.streamUnspentOutputs(lastEpochs))
    }

  private def streamOutputsByErgoTreeTemplateHashR: HttpRoutes[F] =
    defs.streamOutputsByErgoTreeTemplateHashDef.toRoutes { case (template, epochs) =>
      streaming.bytesStream(service.streamOutputsByErgoTreeTemplateHash(template, epochs))
    }

  private def streamUnspentOutputsByErgoTreeTemplateHashR: HttpRoutes[F] =
    defs.streamUnspentOutputsByErgoTreeTemplateHashDef.toRoutes { case (template, epochs) =>
      streaming.bytesStream(service.streamUnspentOutputsByErgoTreeTemplateHash(template, epochs))
    }

  private def outputsByTokenIdR: HttpRoutes[F] =
    defs.outputsByTokenIdDef.toRoutes { case (tokenId, paging) =>
      service.getOutputsByTokenId(tokenId, paging).adaptThrowable.value
    }

  private def unspentOutputsByTokenIdR: HttpRoutes[F] =
    defs.unspentOutputsByTokenIdDef.toRoutes { case (tokenId, paging) =>
      service.getUnspentOutputsByTokenId(tokenId, paging).adaptThrowable.value
    }

  private def getOutputByIdR: HttpRoutes[F] =
    defs.getOutputByIdDef.toRoutes { id =>
      service
        .getOutputById(id)
        .adaptThrowable
        .orNotFound(s"Output with id: $id")
        .value
    }

  private def getOutputsByErgoTreeR: HttpRoutes[F] =
    defs.getOutputsByErgoTreeDef.toRoutes { case (tree, paging) =>
      service.getOutputsByErgoTree(tree, paging).adaptThrowable.value
    }

  private def getUnspentOutputsByErgoTreeR: HttpRoutes[F] =
    defs.getUnspentOutputsByErgoTreeDef.toRoutes { case (tree, paging) =>
      service.getUnspentOutputsByErgoTree(tree, paging).adaptThrowable.value
    }

  private def getOutputsByErgoTreeTemplateHashR: HttpRoutes[F] =
    defs.getOutputsByErgoTreeTemplateHashDef.toRoutes { case (tree, paging) =>
      service.getOutputsByErgoTreeTemplateHash(tree, paging).adaptThrowable.value
    }

  private def getUnspentOutputsByErgoTreeTemplateHashR: HttpRoutes[F] =
    defs.getUnspentOutputsByErgoTreeTemplateHashDef.toRoutes { case (tree, paging) =>
      service.getUnspentOutputsByErgoTreeTemplateHash(tree, paging).adaptThrowable.value
    }

  private def getOutputsByAddressR: HttpRoutes[F] =
    defs.getOutputsByAddressDef.toRoutes { case (address, paging) =>
      service.getOutputsByAddress(address, paging).adaptThrowable.value
    }

  private def getUnspentOutputsByAddressR: HttpRoutes[F] =
    defs.getUnspentOutputsByAddressDef.toRoutes { case (address, paging) =>
      service.getUnspentOutputsByAddress(address, paging).adaptThrowable.value
    }
}

object BoxesRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    settings: RequestsSettings,
    service: BoxesService[F]
  )(implicit opts: Http4sServerOptions[F]): HttpRoutes[F] =
    new BoxesRoutes[F](settings, service).routes
}
