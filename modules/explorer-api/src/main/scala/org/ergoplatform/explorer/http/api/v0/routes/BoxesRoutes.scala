package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{Concurrent, ContextShift, Timer}
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
](service: BoxesService[F, fs2.Stream])(implicit opts: Http4sServerOptions[F, F]) {

  import BoxesEndpointDefs._

  val routes: HttpRoutes[F] =
    getOutputByIdR <+> getOutputsByErgoTreeR <+> getUnspentOutputsByErgoTreeR <+>
    getOutputsByAddressR <+> getUnspentOutputsByAddressR

  private def interpreter = Http4sServerInterpreter(opts)

  private def getOutputByIdR: HttpRoutes[F] =
    interpreter.toRoutes(getOutputByIdDef) { id =>
      service
        .getOutputById(id)
        .adaptThrowable
        .orNotFound(s"Output with id: $id")
        .value
    }

  private def getOutputsByErgoTreeR: HttpRoutes[F] =
    interpreter.toRoutes(getOutputsByErgoTreeDef) { tree =>
      service.getOutputsByErgoTree(tree).compile.toList.adaptThrowable.value
    }

  private def getUnspentOutputsByErgoTreeR: HttpRoutes[F] =
    interpreter.toRoutes(getUnspentOutputsByErgoTreeDef) { tree =>
      service.getUnspentOutputsByErgoTree(tree).compile.toList.adaptThrowable.value
    }

  private def getOutputsByAddressR: HttpRoutes[F] =
    interpreter.toRoutes(getOutputsByAddressDef) { address =>
      service.getOutputsByAddress(address).compile.toList.adaptThrowable.value
    }

  private def getUnspentOutputsByAddressR: HttpRoutes[F] =
    interpreter.toRoutes(getUnspentOutputsByAddressDef) { address =>
      service.getUnspentOutputsByAddress(address).compile.toList.adaptThrowable.value
    }
}

object BoxesRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    service: BoxesService[F, fs2.Stream]
  )(implicit opts: Http4sServerOptions[F, F]): HttpRoutes[F] =
    new BoxesRoutes[F](service).routes
}
