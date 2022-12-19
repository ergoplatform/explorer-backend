package org.ergoplatform.explorer.http.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.ErgoAddressEncoder
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
](settings: RequestsSettings, service: Boxes[F])(implicit
  opts: Http4sServerOptions[F, F],
  e: ErgoAddressEncoder
) {

  val defs = new BoxesEndpointDefs[F](settings)

  val routes: HttpRoutes[F] =
    streamUnspentOutputsByEpochsR <+>
    streamOutputsByGixR <+>
    streamUnspentOutputsByGixR <+>
    streamUnspentOutputsR <+>
    streamOutputsByErgoTreeTemplateHashR <+>
    streamUnspentOutputsByErgoTreeTemplateHashR <+>
    unspentOutputsByTokenIdR <+>
    outputsByTokenIdR <+>
    searchUnspentOutputsByAssetsUnionR <+>
    searchUnspentOutputsR <+>
    searchOutputsR <+>
    getOutputsByErgoTreeR <+>
    getUnspentOutputsByErgoTreeR <+>
    getOutputsByErgoTreeTemplateHashR <+>
    getUnspentOutputsByErgoTreeTemplateHashR <+>
    getOutputsByAddressR <+>
    getUnspentOutputsByAddressR <+>
    getOutputByIdR <+>
    `getUnspent&UnconfirmedOutputsMergedByAddressR` <+>
    getAllUnspentOutputsR

  private def interpreter = Http4sServerInterpreter(opts)

  private def streamUnspentOutputsR: HttpRoutes[F] =
    interpreter.toRoutes(defs.streamUnspentOutputsDef) { epochs =>
      streaming.bytesStream(service.streamUnspentOutputs(epochs))
    }

  private def streamUnspentOutputsByEpochsR: HttpRoutes[F] =
    interpreter.toRoutes(defs.streamUnspentOutputsByEpochsDef) { lastEpochs =>
      streaming.bytesStream(service.streamUnspentOutputs(lastEpochs))
    }

  private def streamUnspentOutputsByGixR: HttpRoutes[F] =
    interpreter.toRoutes(defs.streamUnspentOutputsByGixDef) { case (minGix, limit) =>
      streaming.bytesStream(service.streamUnspentOutputs(minGix, limit))
    }

  private def streamOutputsByGixR: HttpRoutes[F] =
    interpreter.toRoutes(defs.streamOutputsByGixDef) { case (minGix, limit) =>
      streaming.bytesStream(service.streamOutputs(minGix, limit))
    }

  private def streamOutputsByErgoTreeTemplateHashR: HttpRoutes[F] =
    interpreter.toRoutes(defs.streamOutputsByErgoTreeTemplateHashDef) { case (template, epochs) =>
      streaming.bytesStream(service.streamOutputsByErgoTreeTemplateHash(template, epochs))
    }

  private def streamUnspentOutputsByErgoTreeTemplateHashR: HttpRoutes[F] =
    interpreter.toRoutes(defs.streamUnspentOutputsByErgoTreeTemplateHashDef) { case (template, epochs) =>
      streaming.bytesStream(service.streamUnspentOutputsByErgoTreeTemplateHash(template, epochs))
    }

  private def outputsByTokenIdR: HttpRoutes[F] =
    interpreter.toRoutes(defs.outputsByTokenIdDef) { case (tokenId, paging) =>
      service.getOutputsByTokenId(tokenId, paging).adaptThrowable.value
    }

  private def unspentOutputsByTokenIdR: HttpRoutes[F] =
    interpreter.toRoutes(defs.unspentOutputsByTokenIdDef) { case (tokenId, paging, ord) =>
      service.getUnspentOutputsByTokenId(tokenId, paging, ord).adaptThrowable.value
    }

  private def getOutputByIdR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getOutputByIdDef) { id =>
      service
        .getOutputById(id)
        .adaptThrowable
        .orNotFound(s"Output with id: $id")
        .value
    }

  private def getOutputsByErgoTreeR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getOutputsByErgoTreeDef) { case (tree, paging) =>
      service.getOutputsByErgoTree(tree, paging).adaptThrowable.value
    }

  private def getUnspentOutputsByErgoTreeR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getUnspentOutputsByErgoTreeDef) { case (tree, paging, ord) =>
      service.getUnspentOutputsByErgoTree(tree, paging, ord).adaptThrowable.value
    }

  private def getOutputsByErgoTreeTemplateHashR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getOutputsByErgoTreeTemplateHashDef) { case (tree, paging) =>
      service.getOutputsByErgoTreeTemplateHash(tree, paging).adaptThrowable.value
    }

  private def getUnspentOutputsByErgoTreeTemplateHashR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getUnspentOutputsByErgoTreeTemplateHashDef) { case (tree, paging) =>
      service.getUnspentOutputsByErgoTreeTemplateHash(tree, paging).adaptThrowable.value
    }

  private def getOutputsByAddressR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getOutputsByAddressDef) { case (address, paging) =>
      service.getOutputsByAddress(address, paging).adaptThrowable.value
    }

  private def getUnspentOutputsByAddressR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getUnspentOutputsByAddressDef) { case (address, paging, ord) =>
      service.getUnspentOutputsByAddress(address, paging, ord).adaptThrowable.value
    }

  private def `getUnspent&UnconfirmedOutputsMergedByAddressR`: HttpRoutes[F] =
    interpreter.toRoutes(defs.`getUnspent&UnconfirmedOutputsMergedByAddressDef`) { case (address, sorting) =>
      service
        .`getUnspent&UnconfirmedOutputsMergedByAddress`(
          address,
          sorting
        )
        .adaptThrowable
        .value
    }

  private def searchOutputsR: HttpRoutes[F] =
    interpreter.toRoutes(defs.searchOutputsDef) { case (query, paging) =>
      service.searchAll(query, paging).adaptThrowable.value
    }

  private def searchUnspentOutputsR: HttpRoutes[F] =
    interpreter.toRoutes(defs.searchUnspentOutputsDef) { case (query, paging) =>
      service.searchUnspent(query, paging).adaptThrowable.value
    }

  private def searchUnspentOutputsByAssetsUnionR: HttpRoutes[F] =
    interpreter.toRoutes(defs.searchUnspentOutputsByTokensUnionDef) { case (query, paging) =>
      service.searchUnspentByAssetsUnion(query, paging).adaptThrowable.value
    }

  private def getAllUnspentOutputsR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getAllUnspentOutputsByAddressDef) { case (address, paging, ord) =>
      service.getAllUnspentOutputs(address, paging, ord).adaptThrowable.value
    }
}

object BoxesRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    settings: RequestsSettings,
    service: Boxes[F]
  )(implicit opts: Http4sServerOptions[F, F], e: ErgoAddressEncoder): HttpRoutes[F] =
    new BoxesRoutes[F](settings, service).routes
}
