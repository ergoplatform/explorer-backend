package org.ergoplatform.explorer.http.api.v0.modules

import cats.Monad
import org.ergoplatform.explorer.http.api.v0.models.SearchResult
import org.ergoplatform.explorer.http.api.v0.services.{AddressesService, BlockChainService, TransactionsService}
import tofu.Start
import tofu.syntax.monadic._
import tofu.syntax.start._

trait Search[F[_]] {

  def search(query: String): F[SearchResult]
}

object Search {

  def apply[F[_]: Monad: Start](
    blocks: BlockChainService[F],
    transactions: TransactionsService[F],
    addresses: AddressesService[F, fs2.Stream]
  ): Search[F] =
    new Search[F] {

      def search(query: String): F[SearchResult] =
        for {
          blocksF    <- blocks.getBlocksByIdLike(query).start
          txsF       <- transactions.getIdsLike(query).start
          addressesF <- addresses.getAllLike(query).start
          blocks     <- blocksF.join
          txs        <- txsF.join
          addresses  <- addressesF.join
        } yield SearchResult(blocks, txs, addresses)
    }
}
