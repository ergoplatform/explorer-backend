package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{Concurrent, ContextShift, Sync, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v0.services.DexService
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class DexRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](service: DexService[F, fs2.Stream])(
  implicit opts: Http4sServerOptions[F, F]
) {

  import org.ergoplatform.explorer.http.api.v0.defs.DexEndpointsDefs._

  val routes: HttpRoutes[F] =
    getUnspentSellOrdersR <+> getUnspentBuyOrdersR

  private def interpreter = Http4sServerInterpreter(opts)

  private def getUnspentSellOrdersR: HttpRoutes[F] =
    interpreter.toRoutes(getUnspentSellOrdersDef) {
      case (tokenId, paging) =>
        service
          .getUnspentSellOrders(tokenId, paging)
          .compile
          .toList
          .adaptThrowable
          .value
    }

  private def getUnspentBuyOrdersR: HttpRoutes[F] =
    interpreter.toRoutes(getUnspentBuyOrdersDef) {
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

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    service: DexService[F, fs2.Stream]
  )(implicit opts: Http4sServerOptions[F, F]): HttpRoutes[F] =
    new DexRoutes(service).routes
}
