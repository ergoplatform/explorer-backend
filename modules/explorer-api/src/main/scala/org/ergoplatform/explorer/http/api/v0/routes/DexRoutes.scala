package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{ContextShift, Sync}
import cats.syntax.semigroupk._
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v0.services.DexService
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class DexRoutes[
  F[_]: Sync: ContextShift: AdaptThrowableEitherT[*[_], ApiErr]
](service: DexService[F, fs2.Stream]) {

  import org.ergoplatform.explorer.http.api.v0.defs.DexEndpointsDefs._

  val routes: HttpRoutes[F] =
    getUnspentSellOrdersR <+> getUnspentBuyOrdersR

  private def getUnspentSellOrdersR: HttpRoutes[F] =
    getUnspentSellOrdersDef.toRoutes {
      case (tokenId, paging) =>
        service
          .getUnspentSellOrders(tokenId, paging)
          .compile
          .toList
          .adaptThrowable
          .value
    }

  private def getUnspentBuyOrdersR: HttpRoutes[F] =
    getUnspentBuyOrdersDef.toRoutes {
      case (tokenId, paging) =>
        service
          .getUnspentBuyOrders(tokenId, paging)
          .compile
          .toList
          .adaptThrowable
          .value
    }
}

object DexRoutes {

  def apply[F[_]: Sync: ContextShift](
    service: DexService[F, fs2.Stream]
  ): HttpRoutes[F] =
    new DexRoutes(service).routes
}
