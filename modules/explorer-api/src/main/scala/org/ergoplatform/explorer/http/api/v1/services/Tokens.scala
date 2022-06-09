package org.ergoplatform.explorer.http.api.v1.services

import cats.effect.Sync
import cats.{FlatMap, Monad}
import mouse.anyf._
import org.ergoplatform.explorer.{CRaise, TokenId, TokenName, TokenSymbol}
import org.ergoplatform.explorer.Err.RequestProcessingErr.InconsistentDbData
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.Sorting.SortOrder
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.v1.TokenStatus
import org.ergoplatform.explorer.http.api.v1.models.{CheckTokenInfo, GenuineTokenInfo, TokenInfo}
import org.ergoplatform.explorer.http.api.v1.utils.TokenVerificationOptionT
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

  /** Check token verification status
    */
  def checkToken(tokenId: TokenId, tokenName: TokenName): F[CheckTokenInfo]

  /** Get all genuine tokens (Eip0021)
    */
  def getGenuineTokenList(paging: Paging): F[Items[GenuineTokenInfo]]

  /** Get all blocked tokens (Eip0021)
    */
  def getBlockedTokenList(paging: Paging): F[Items[String]]

}

object Tokens {

  def apply[
    F[_]: Sync,
    D[_]: LiftConnectionIO: CRaise[*[_], InconsistentDbData]: Monad
  ](trans: D Trans F): F[Tokens[F]] =
    (TokenRepo[F, D], GenuineTokenRepo[F, D], BlockedTokenRepo[F, D]).mapN(new Live(_, _, _)(trans))

  final private class Live[
    F[_]: FlatMap,
    D[_]: LiftConnectionIO: CRaise[*[_], InconsistentDbData]: Monad
  ](tokenRepo: TokenRepo[D], genuineTokenRepo: GenuineTokenRepo[D], blockedTokenRepo: BlockedTokenRepo[D])(
    trans: D Trans F
  ) extends Tokens[F] {

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
      tokenRepo
        .countAll(hideNfts)
        .flatMap { total =>
          tokenRepo
            .getAll(paging.offset, paging.limit, ordering.value, hideNfts)
            .map(_.map(TokenInfo(_)))
            .map(Items(_, total))
        }
        .thrushK(trans.xa)

    def checkToken(tokenId: TokenId, tokenName: TokenName): F[CheckTokenInfo] =
      (
        for {
          genuineT  <- genuineTokenRepo.get(tokenId)
          blockedT  <- blockedTokenRepo.get(tokenId)
          genuineTs <- genuineTokenRepo.getByNameAndUniqueOP(tokenName, unique = true)
          ops = TokenVerificationOptionT(genuineT, blockedT, genuineTs)
        } yield CheckTokenInfo(ops.getOrElse(TokenStatus.parse(0)), genuineT.map(GenuineTokenInfo(_)))
      ) ||> trans.xa

    def getGenuineTokenList(paging: Paging): F[Items[GenuineTokenInfo]] =
      (for {
        total <- genuineTokenRepo.countAll
        gts   <- genuineTokenRepo.getAll(paging.offset, paging.limit).map(_.map(GenuineTokenInfo(_)))
      } yield Items(gts, total)) ||> trans.xa

    def getBlockedTokenList(paging: Paging): F[Items[String]] =
      (for {
        total <- blockedTokenRepo.countAll
        bts   <- blockedTokenRepo.getAll(paging.offset, paging.limit).map(_.map(_.tokenName))
      } yield Items(bts, total)) ||> trans.xa
  }
}
