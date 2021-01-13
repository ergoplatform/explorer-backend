package org.ergoplatform.explorer.http.api.v1.services

import cats.effect.Sync
import cats.{FlatMap, Monad}
import fs2.Stream
import mouse.anyf._
import org.ergoplatform.explorer.CRaise
import org.ergoplatform.explorer.Err.RequestProcessingErr.InconsistentDbData
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.v1.models.{AssetInfo, TokenInfo}
import tofu.syntax.monadic._

/** A service providing an access to the assets data.
  */
trait AssetsService[F[_], S[_[_], _]] {

  /** Get all assets matching a given `query`.
    */
  def getAllLike(idSubstring: String, paging: Paging): F[Items[AssetInfo]]

  /** Get all issued tokens.
    */
  def getTokens(paging: Paging): F[Items[TokenInfo]]
}

object AssetsService {

  def apply[
    F[_]: Sync,
    D[_]: LiftConnectionIO: CRaise[*[_], InconsistentDbData]: Monad
  ](trans: D Trans F): F[AssetsService[F, Stream]] =
    (AssetRepo[F, D], TokenRepo[F, D]).mapN(new Live(_, _)(trans))

  final private class Live[
    F[_]: FlatMap,
    D[_]: CRaise[*[_], InconsistentDbData]: Monad
  ](assetRepo: AssetRepo[D, Stream], tokenRepo: TokenRepo[D])(trans: D Trans F)
    extends AssetsService[F, Stream] {

    def getAllLike(idSubstring: String, paging: Paging): F[Items[AssetInfo]] =
      assetRepo
        .countAllLike(idSubstring)
        .flatMap { total =>
          assetRepo
            .getAllLike(idSubstring, paging.offset, paging.limit)
            .map(_.map(AssetInfo(_)))
            .map(Items(_, total))
        }
        .thrushK(trans.xa)

    def getTokens(paging: Paging): F[Items[TokenInfo]] =
      tokenRepo.countAll
        .flatMap { total =>
          tokenRepo
            .getAll(paging.offset, paging.limit)
            .map(_.map(TokenInfo.apply))
            .map(Items(_, total))
        }
        .thrushK(trans.xa)
  }
}
