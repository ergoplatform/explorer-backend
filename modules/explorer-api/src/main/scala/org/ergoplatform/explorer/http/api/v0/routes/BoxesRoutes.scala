package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{ContextShift, Sync}
import cats.syntax.flatMap._
import cats.syntax.semigroupk._
import cats.syntax.option._
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v0.defs.BoxesEndpointDefs
import org.ergoplatform.explorer.http.api.v0.services.BoxesService
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class BoxesRoutes[
  F[_]: Sync: ContextShift: AdaptThrowableEitherT[*[_], ApiErr]
](service: BoxesService[F, fs2.Stream]) {

  import BoxesEndpointDefs._

  val routes: HttpRoutes[F] =
    getOutputByIdR <+> getOutputsByErgoTreeR <+> getUnspentOutputsByErgoTreeR <+>
    getOutputsByAddressR <+> getUnspentOutputsByAddressR

  private def getOutputByIdR: HttpRoutes[F] =
    getOutputByIdDef.toRoutes { id =>
      service
        .getOutputById(id)
        .flatMap(_.liftTo[F](ApiErr.NotFound(s"Output with id: $id")))
        .adaptThrowable
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
      service.getOutputsByErgoTree(address).compile.toList.adaptThrowable.value
    }

  private def getUnspentOutputsByAddressR: HttpRoutes[F] =
    getUnspentOutputsByAddressDef.toRoutes { address =>
      service.getUnspentOutputsByErgoTree(address).compile.toList.adaptThrowable.value
    }
}

object BoxesRoutes {

  def apply[F[_]: Sync: ContextShift](
    service: BoxesService[F, fs2.Stream]
  ): HttpRoutes[F] =
    new BoxesRoutes[F](service).routes
}
