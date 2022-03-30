package org.ergoplatform.explorer.http.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v1.defs.AddressesEndpointDefs
import org.ergoplatform.explorer.http.api.v1.services.{Addresses, Transactions}
import org.ergoplatform.explorer.settings.RequestsSettings
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class AddressesRoutes[F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]](
  settings: RequestsSettings,
  transactions: Transactions[F],
  addresses: Addresses[F]
)(implicit opts: Http4sServerOptions[F, F]) {

  val defs = new AddressesEndpointDefs(settings)

  val routes: HttpRoutes[F] = getTxsByAddressR <+> getConfirmedBalanceR <+> getTotalBalanceR

  private def interpreter = Http4sServerInterpreter(opts)

  private def getTxsByAddressR =
    interpreter.toRoutes(defs.getTxsByAddressDef) { case (addr, paging, concise) =>
      transactions.getByAddress(addr, paging, concise).adaptThrowable.value
    }

  private def getConfirmedBalanceR =
    interpreter.toRoutes(defs.getConfirmedBalanceDef) { case (addr, confirmations) =>
      addresses.confirmedBalanceOf(addr, confirmations).adaptThrowable.value
    }

  private def getTotalBalanceR =
    interpreter.toRoutes(defs.getTotalBalanceDef) { addr =>
      addresses.totalBalanceOf(addr).adaptThrowable.value
    }
}

object AddressesRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    settings: RequestsSettings,
    transactions: Transactions[F],
    addresses: Addresses[F]
  )(implicit opts: Http4sServerOptions[F, F]): HttpRoutes[F] =
    new AddressesRoutes[F](settings, transactions, addresses).routes
}
