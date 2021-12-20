package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.syntax.routes._
import org.ergoplatform.explorer.http.api.v0.services.{OffChainService, TransactionsService}
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class TransactionsRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](txsService: TransactionsService[F], offChainService: OffChainService[F])(implicit
  opts: Http4sServerOptions[F, F]
) {

  import org.ergoplatform.explorer.http.api.v0.defs.TransactionsEndpointDefs._

  val routes: HttpRoutes[F] =
    getUnconfirmedTxsByAddressR <+> getUnconfirmedTxByIdR <+> getUnconfirmedTxsR <+>
    getTxsSinceR <+> sendTransactionR <+> getTxByIdR

  private def interpreter = Http4sServerInterpreter(opts)

  private def getTxByIdR: HttpRoutes[F] =
    interpreter.toRoutes(getTxByIdDef) { txId =>
      txsService
        .getTxInfo(txId)
        .adaptThrowable
        .orNotFound(s"Transaction with id: $txId")
        .value
    }

  private def getUnconfirmedTxsR: HttpRoutes[F] =
    interpreter.toRoutes(getUnconfirmedTxsDef) {
      case (paging, sorting) =>
      offChainService
        .getUnconfirmedTxs(paging, sorting)
        .adaptThrowable
        .value
    }

  private def getUnconfirmedTxByIdR: HttpRoutes[F] =
    interpreter.toRoutes(getUnconfirmedTxByIdDef) { txId =>
      offChainService
        .getUnconfirmedTxInfo(txId)
        .adaptThrowable
        .orNotFound(s"Unconfirmed transaction with id: $txId")
        .value
    }

  private def getUnconfirmedTxsByAddressR: HttpRoutes[F] =
    interpreter.toRoutes(getUnconfirmedTxsByAddressDef) { case (paging, address) =>
      offChainService
        .getUnconfirmedTxsByAddress(address, paging)
        .adaptThrowable
        .value
    }

  private def getTxsSinceR: HttpRoutes[F] =
    interpreter.toRoutes(getTxsSinceDef) { case (paging, height) =>
      txsService
        .getTxsSince(height, paging)
        .adaptThrowable
        .value
    }

  private def sendTransactionR: HttpRoutes[F] =
    interpreter.toRoutes(sendTransactionDef) { tx =>
      offChainService.submitTransaction(tx).adaptThrowable.value
    }
}

object TransactionsRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    txsService: TransactionsService[F],
    offChainService: OffChainService[F]
  )(implicit opts: Http4sServerOptions[F, F]): HttpRoutes[F] =
    new TransactionsRoutes(txsService, offChainService).routes
}
