package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{ContextShift, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.explorer.http.api.v0.services.{
  AddressesService,
  BlockChainService,
  TransactionsService
}
import org.http4s.HttpRoutes
import org.ergoplatform.explorer.http.api.syntax.applicativeThrow._
import org.ergoplatform.explorer.http.api.v0.models.SearchResult
import sttp.tapir.server.http4s._

final class SearchRoutes[F[_]: Sync: ContextShift: Logger](
  blocksService: BlockChainService[F, fs2.Stream],
  txsService: TransactionsService[F, fs2.Stream],
  addressesService: AddressesService[F, fs2.Stream]
) {

  import org.ergoplatform.explorer.http.api.v0.defs.SearchEndpointDefs._

  val routes: HttpRoutes[F] = searchR

  private def searchR: HttpRoutes[F] =
    searchDef.toRoutes { q =>
      (for {
        blocks    <- blocksService.getBlocksByIdLike(q)
        txs       <- txsService.getIdsLike(q)
        addresses <- addressesService.getAllLike(q)
      } yield SearchResult(blocks, txs, addresses)).attemptApi
    }
}

object SearchRoutes {

  def apply[F[_]: Sync: ContextShift](
    blocksService: BlockChainService[F, fs2.Stream],
    txsService: TransactionsService[F, fs2.Stream],
    addressesService: AddressesService[F, fs2.Stream]
  ): F[HttpRoutes[F]] =
    Slf4jLogger.create.map { implicit logger =>
      new SearchRoutes(blocksService, txsService, addressesService).routes
    }
}
