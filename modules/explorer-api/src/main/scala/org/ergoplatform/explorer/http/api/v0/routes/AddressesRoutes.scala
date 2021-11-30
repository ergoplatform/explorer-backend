package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{Concurrent, ContextShift, Sync, Timer}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.semigroupk._
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.models.Items
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v0.services.{AddressesService, TransactionsService}
import org.http4s.HttpRoutes
import sttp.tapir._
import sttp.tapir.server.http4s._

final class AddressesRoutes[
  F[_]: Concurrent: Timer: ContextShift: AdaptThrowableEitherT[*[_], ApiErr]
](
  addressesService: AddressesService[F, Stream],
  transactionsService: TransactionsService[F]
)(implicit opts: Http4sServerOptions[F, F]) {

  import org.ergoplatform.explorer.http.api.v0.defs.AddressesEndpointDefs._

  private def interpreter = Http4sServerInterpreter(opts)

  val routes: HttpRoutes[F] =
    getAddressR <+> getTxsByAddressR <+> getAssetHoldersR <+> getBalancesR

  def getAddressR: HttpRoutes[F] =
    interpreter.toRoutes(getAddressDef) {
      case (address, minConfirmations) =>
        addressesService.getAddressInfo(address, minConfirmations).adaptThrowable.value
    }

  def getTxsByAddressR: HttpRoutes[F] =
    interpreter.toRoutes(getTxsByAddressDef) {
      case (address, paging, concise) =>
        transactionsService
          .getTxsInfoByAddress(address, paging, concise)
          .adaptThrowable
          .value
    }

  def getAssetHoldersR: HttpRoutes[F] =
    interpreter.toRoutes(getAssetHoldersDef) {
      case (tokenId, paging) =>
        addressesService
          .getAssetHoldersAddresses(tokenId, paging)
          .compile
          .toList
          .adaptThrowable
          .value
    }

  def getBalancesR: HttpRoutes[F] =
    interpreter.toRoutes(getBalancesDef) { paging =>
      addressesService.balances(paging).adaptThrowable.value
    }
}

object AddressesRoutes {

  def apply[F[_]: Concurrent: Timer: ContextShift: Logger](
    addressesService: AddressesService[F, Stream],
    transactionsService: TransactionsService[F]
  )(implicit opts: Http4sServerOptions[F, F]): HttpRoutes[F] =
    new AddressesRoutes(addressesService, transactionsService).routes
}
