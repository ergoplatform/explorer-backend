package org.ergoplatform.explorer.http.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v1.defs.MempoolEndpointDefs
import org.ergoplatform.explorer.http.api.v1.services.Mempool
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class MempoolRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](service: Mempool[F])(implicit opts: Http4sServerOptions[F, F]) {

  val defs = new MempoolEndpointDefs()

  val routes: HttpRoutes[F] = sendTransactionR <+> getTransactionsByAddressR

  private def interpreter = Http4sServerInterpreter(opts)

  private def sendTransactionR: HttpRoutes[F] =
    interpreter.toRoutes(defs.sendTransactionDef) { tx =>
      service.submit(tx).adaptThrowable.value
    }

  private def getTransactionsByAddressR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getTransactionsByAddressDef) { case (address, paging) =>
      service.getByAddress(address, paging).adaptThrowable.value
    }
}

object MempoolRoutes {

  def apply[
    F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
  ](service: Mempool[F])(implicit opts: Http4sServerOptions[F, F]): HttpRoutes[F] =
    new MempoolRoutes[F](service).routes
}
