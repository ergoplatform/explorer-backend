package org.ergoplatform.explorer.http.api.v1.services

import cats.effect.Sync
import cats.{FlatMap, Monad}
import fs2.Stream
import mouse.anyf._
import org.ergoplatform.explorer.{CRaise, TokenId, TokenSymbol}
import org.ergoplatform.explorer.Err.RequestProcessingErr.InconsistentDbData
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.Sorting.SortOrder
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.v1.models.TokenInfo
import tofu.syntax.monadic._
import tofu.syntax.foption._

/** A service providing an access to the assets data.
  */
trait Tokens[F[_]] {

  def get(id: TokenId): F[Option[TokenInfo]]

  def getBySymbol(sym: TokenSymbol): F[List[TokenInfo]]

  /** Get all assets matching a given `query`.
    */
  def search(q: String, paging: Paging): F[Items[TokenInfo]]

  /** Get all issued tokens.
    */
  def getAll(paging: Paging, ordering: SortOrder, hideNfts: Boolean): F[Items[TokenInfo]]
}

object Tokens {

  def apply[
    F[_]: Sync,
    D[_]: LiftConnectionIO: CRaise[*[_], InconsistentDbData]: Monad
  ](trans: D Trans F): F[Tokens[F]] =
    TokenRepo[F, D].map(new Live(_)(trans))

  final private class Live[
    F[_]: FlatMap,
    D[_]: CRaise[*[_], InconsistentDbData]: Monad
  ](tokenRepo: TokenRepo[D])(trans: D Trans F)
    extends Tokens[F] {

    def get(id: TokenId): F[Option[TokenInfo]] =
      tokenRepo.get(id).mapIn(TokenInfo(_)).thrushK(trans.xa)

    def getBySymbol(sym: TokenSymbol): F[List[TokenInfo]] =
      tokenRepo.getBySymbol(sym).map(_.map(TokenInfo(_))).thrushK(trans.xa)

    def search(q: String, paging: Paging): F[Items[TokenInfo]] =
      tokenRepo
        .countAllLike(q)
        .flatMap { total =>
          tokenRepo
            .getAllLike(q, paging.offset, paging.limit)
            .map(_.map(TokenInfo(_)))
            .map(Items(_, total))
        }
        .thrushK(trans.xa)

    def getAll(paging: Paging, ordering: SortOrder, hideNfts: Boolean): F[Items[TokenInfo]] =
      tokenRepo.countAll(hideNfts)
        .flatMap { total =>
          tokenRepo
            .getAll(paging.offset, paging.limit, ordering.value, hideNfts)
            .map(_.map(TokenInfo(_)))
            .map(Items(_, total))
        }
        .thrushK(trans.xa)
  }
}
