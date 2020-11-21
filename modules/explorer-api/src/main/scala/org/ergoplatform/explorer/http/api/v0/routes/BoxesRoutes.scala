package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{Concurrent, ContextShift, Sync, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.syntax.routes._
import org.ergoplatform.explorer.http.api.v0.defs.BoxesEndpointDefs
import org.ergoplatform.explorer.http.api.v0.services.BoxesService
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class BoxesRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](service: BoxesService[F, fs2.Stream])(implicit opts: Http4sServerOptions[F]) {

  import BoxesEndpointDefs._

  val routes: HttpRoutes[F] =
    getOutputByIdR <+> getOutputsByErgoTreeR <+> getUnspentOutputsByErgoTreeR <+>
    getOutputsByAddressR <+> getUnspentOutputsByAddressR

  private def getOutputByIdR: HttpRoutes[F] =
    getOutputByIdDef.toRoutes { id =>
      service
        .getOutputById(id)
        .adaptThrowable
        .orNotFound(s"Output with id: $id")
        .value
    }

  private def getOutputsByErgoTreeR: HttpRoutes[F] =
    getOutputsByErgoTreeDef.toRoutes { tree =>
      service.getOutputsByErgoTree(tree).compile.toList.adaptThrowable.value
    }

  private def getUnspentOutputsByErgoTreeR: HttpRoutes[F] =
    getUnspentOutputsByErgoTreeDef.toRoutes { tree =>
      service.getUnspentOutputsByErgoTree(tree).compile.toList.adaptThrowable.value
    }

  private def getOutputsByAddressR: HttpRoutes[F] =
    getOutputsByAddressDef.toRoutes { address =>
      service.getOutputsByAddress(address).compile.toList.adaptThrowable.value
    }

  private def getUnspentOutputsByAddressR: HttpRoutes[F] =
    getUnspentOutputsByAddressDef.toRoutes { address =>
      service.getUnspentOutputsByAddress(address).compile.toList.adaptThrowable.value
    }
}

object BoxesRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    service: BoxesService[F, fs2.Stream]
  )(implicit opts: Http4sServerOptions[F]): HttpRoutes[F] =
    new BoxesRoutes[F](service).routes
}
