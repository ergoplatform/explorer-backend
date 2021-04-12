package org.ergoplatform.explorer.http.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v1.defs.AddressesEndpointDefs
import org.ergoplatform.explorer.http.api.v1.services.Transactions
import org.ergoplatform.explorer.settings.RequestsSettings
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class AddressesRoutes[F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]](
  settings: RequestsSettings,
  transactionsService: Transactions[F]
) {

  val defs = new AddressesEndpointDefs(settings)

  val routes: HttpRoutes[F] = getTxsByAddressR

  private def getTxsByAddressR =
    Http4sServerInterpreter.toRoutes(defs.getTxsByAddressDef) { case (addr, paging) =>
      transactionsService.getByAddress(addr, paging).adaptThrowable.value
    }
}

object AddressesRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    settings: RequestsSettings,
    service: Transactions[F]
  )(implicit opts: Http4sServerOptions[F, F]): HttpRoutes[F] =
    new AddressesRoutes[F](settings, service).routes
}
