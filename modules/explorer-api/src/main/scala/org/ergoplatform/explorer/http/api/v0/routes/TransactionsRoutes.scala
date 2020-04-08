package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{ContextShift, Sync}
import cats.syntax.flatMap._
import cats.syntax.option._
import cats.syntax.semigroupk._
import fs2.Stream
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v0.services.TransactionsService
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class TransactionsRoutes[
  F[_]: Sync: ContextShift: AdaptThrowableEitherT[*[_], ApiErr]
](service: TransactionsService[F, Stream])(
  implicit opts: Http4sServerOptions[F]
) {

  import org.ergoplatform.explorer.http.api.v0.defs.TransactionsEndpointDefs._

  val routes: HttpRoutes[F] =
    getTxByIdR <+> getUnconfirmedTxByIdR <+> getTxsSinceR <+> sendTransactionR

  private def getTxByIdR: HttpRoutes[F] =
    getTxByIdDef.toRoutes { txId =>
      service
        .getTxInfo(txId)
        .flatMap(_.liftTo[F](ApiErr.NotFound(s"Transaction with id: $txId")))
        .adaptThrowable
        .value
    }

  private def getUnconfirmedTxByIdR: HttpRoutes[F] =
    getUnconfirmedTxByIdDef.toRoutes { txId =>
      service
        .getUnconfirmedTxInfo(txId)
        .flatMap(_.liftTo[F](ApiErr.NotFound(s"Unconfirmed transaction with id: $txId")))
        .adaptThrowable
        .value
    }

  private def getTxsSinceR: HttpRoutes[F] =
    getTxsSinceDef.toRoutes {
      case (paging, height) =>
        service
          .getTxsSince(height, paging)
          .compile
          .toList
          .adaptThrowable
          .value
    }

  private def sendTransactionR: HttpRoutes[F] =
    sendTransactionDef.toRoutes { tx =>
      service.submitTransaction(tx).adaptThrowable.value
    }
}

object TransactionsRoutes {

  def apply[F[_]: Sync: ContextShift](
    service: TransactionsService[F, Stream]
  )(implicit opts: Http4sServerOptions[F]): HttpRoutes[F] =
    new TransactionsRoutes(service).routes
}
