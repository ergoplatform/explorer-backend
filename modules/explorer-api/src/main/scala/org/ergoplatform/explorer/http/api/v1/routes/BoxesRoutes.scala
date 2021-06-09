package org.ergoplatform.explorer.http.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.syntax.routes._
import org.ergoplatform.explorer.http.api.v1.defs.BoxesEndpointDefs
import org.ergoplatform.explorer.http.api.v1.services.Boxes
import org.ergoplatform.explorer.http.api.{streaming, ApiErr}
import org.ergoplatform.explorer.settings.RequestsSettings
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class BoxesRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](settings: RequestsSettings, service: Boxes[F])(implicit opts: Http4sServerOptions[F, F]) {

  val defs = new BoxesEndpointDefs[F](settings)

  val routes: HttpRoutes[F] =
    streamUnspentOutputsByEpochsR <+>
    streamUnspentOutputsByGixR <+>
    streamUnspentOutputsR <+>
    streamOutputsByErgoTreeTemplateHashR <+>
    streamUnspentOutputsByErgoTreeTemplateHashR <+>
    unspentOutputsByTokenIdR <+>
    outputsByTokenIdR <+>
    searchOutputsR <+>
    getOutputsByErgoTreeR <+>
    getUnspentOutputsByErgoTreeR <+>
    getOutputsByErgoTreeTemplateHashR <+>
    getUnspentOutputsByErgoTreeTemplateHashR <+>
    getOutputsByAddressR <+>
    getUnspentOutputsByAddressR <+>
    getOutputByIdR

  private def streamUnspentOutputsR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(defs.streamUnspentOutputsDef) { epochs =>
      streaming.bytesStream(service.streamUnspentOutputs(epochs))
    }

  private def streamUnspentOutputsByEpochsR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(defs.streamUnspentOutputsByEpochsDef) { lastEpochs =>
      streaming.bytesStream(service.streamUnspentOutputs(lastEpochs))
    }

  private def streamUnspentOutputsByGixR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(defs.streamUnspentOutputsByGixDef) { case (minGix, limit) =>
      streaming.bytesStream(service.streamUnspentOutputs(minGix, limit))
    }

  private def streamOutputsByErgoTreeTemplateHashR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(defs.streamOutputsByErgoTreeTemplateHashDef) { case (template, epochs) =>
      streaming.bytesStream(service.streamOutputsByErgoTreeTemplateHash(template, epochs))
    }

  private def streamUnspentOutputsByErgoTreeTemplateHashR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(defs.streamUnspentOutputsByErgoTreeTemplateHashDef) { case (template, epochs) =>
      streaming.bytesStream(service.streamUnspentOutputsByErgoTreeTemplateHash(template, epochs))
    }

  private def outputsByTokenIdR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(defs.outputsByTokenIdDef) { case (tokenId, paging) =>
      service.getOutputsByTokenId(tokenId, paging).adaptThrowable.value
    }

  private def unspentOutputsByTokenIdR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(defs.unspentOutputsByTokenIdDef) { case (tokenId, paging) =>
      service.getUnspentOutputsByTokenId(tokenId, paging).adaptThrowable.value
    }

  private def getOutputByIdR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(defs.getOutputByIdDef) { id =>
      service
        .getOutputById(id)
        .adaptThrowable
        .orNotFound(s"Output with id: $id")
        .value
    }

  private def getOutputsByErgoTreeR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(defs.getOutputsByErgoTreeDef) { case (tree, paging) =>
      service.getOutputsByErgoTree(tree, paging).adaptThrowable.value
    }

  private def getUnspentOutputsByErgoTreeR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(defs.getUnspentOutputsByErgoTreeDef) { case (tree, paging) =>
      service.getUnspentOutputsByErgoTree(tree, paging).adaptThrowable.value
    }

  private def getOutputsByErgoTreeTemplateHashR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(defs.getOutputsByErgoTreeTemplateHashDef) { case (tree, paging) =>
      service.getOutputsByErgoTreeTemplateHash(tree, paging).adaptThrowable.value
    }

  private def getUnspentOutputsByErgoTreeTemplateHashR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(defs.getUnspentOutputsByErgoTreeTemplateHashDef) { case (tree, paging) =>
      service.getUnspentOutputsByErgoTreeTemplateHash(tree, paging).adaptThrowable.value
    }

  private def getOutputsByAddressR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(defs.getOutputsByAddressDef) { case (address, paging) =>
      service.getOutputsByAddress(address, paging).adaptThrowable.value
    }

  private def getUnspentOutputsByAddressR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(defs.getUnspentOutputsByAddressDef) { case (address, paging) =>
      service.getUnspentOutputsByAddress(address, paging).adaptThrowable.value
    }

  private def searchOutputsR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(defs.searchOutputsDef) { case (query, paging) =>
      service.searchAll(query, paging).adaptThrowable.value
    }
}

object BoxesRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    settings: RequestsSettings,
    service: Boxes[F]
  )(implicit opts: Http4sServerOptions[F, F]): HttpRoutes[F] =
    new BoxesRoutes[F](settings, service).routes
}
