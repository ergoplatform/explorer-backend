package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{ContextShift, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.semigroupk._
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.explorer.http.api.models.Items
import org.ergoplatform.explorer.http.api.syntax.applicativeThrow._
import org.ergoplatform.explorer.http.api.v0.services.{AddressesService, TransactionsService}
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class AddressesRoutes[F[_]: Sync: ContextShift: Logger](
  addressesService: AddressesService[F, Stream],
  transactionsService: TransactionsService[F, Stream]
) {

  import org.ergoplatform.explorer.http.api.v0.defs.AddressesEndpointDefs._

  val routes: HttpRoutes[F] =
    getAddressR <+> getTxsByAddressR <+> getAssetHoldersR

  def getAddressR: HttpRoutes[F] =
    getAddressDef.toRoutes { address =>
      addressesService.getAddressInfo(address).either
    }

  def getTxsByAddressR: HttpRoutes[F] =
    getTxsByAddressDef.toRoutes {
      case (address, paging) =>
        transactionsService
          .countTxsInfoByAddress(address)
          .flatMap { totalNumTxs =>
            transactionsService
              .getTxsInfoByAddress(address, paging)
              .compile
              .toList
              .map(Items(_, totalNumTxs))
              .either
          }
    }

  def getAssetHoldersR: HttpRoutes[F] =
    getAssetHoldersDef.toRoutes {
      case (tokenId, paging) =>
        addressesService
          .getAssetHoldersAddresses(tokenId, paging)
          .compile
          .toList
          .either
    }
}

object AddressesRoutes {

  def apply[F[_]: Sync: ContextShift](
    addressesService: AddressesService[F, Stream],
    transactionsService: TransactionsService[F, Stream]
  ): F[HttpRoutes[F]] =
    Slf4jLogger.create.map { implicit logger =>
      new AddressesRoutes(addressesService, transactionsService).routes
    }
}
