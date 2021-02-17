package org.ergoplatform.explorer.http.api.v0.services

import cats.Monad
import cats.effect.Sync
import cats.syntax.apply._
import fs2.Stream
import mouse.anyf._
import org.ergoplatform.explorer.Err.RefinementFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.DexErr
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.Paging
import org.ergoplatform.explorer.http.api.v0.models.{DexBuyOrderInfo, DexSellOrderInfo}
import org.ergoplatform.explorer.protocol.dex
import org.ergoplatform.explorer.syntax.stream._
import org.ergoplatform.explorer.{CRaise, TokenId}

/** A service providing an access to the DEX data.
  */
trait DexService[F[_], S[_[_], _]] {

  def getUnspentSellOrders(tokenId: TokenId, paging: Paging): S[F, DexSellOrderInfo]

  def getUnspentBuyOrders(tokenId: TokenId, paging: Paging): S[F, DexBuyOrderInfo]
}

object DexService {

  def apply[
    F[_]: Sync,
    D[_]: LiftConnectionIO: Monad: CRaise[*[_], DexErr]: CRaise[*[_], RefinementFailed]
  ](trans: D Trans F): F[DexService[F, Stream]] =
    (OutputRepo[F, D], AssetRepo[F, D]).mapN(new Live(_, _)(trans))

  final private class Live[
    F[_],
    D[_]: Monad: CRaise[*[_], DexErr]: CRaise[*[_], RefinementFailed]
  ](
    outputRepo: OutputRepo[D, Stream],
    assetRepo: AssetRepo[D, Stream]
  )(trans: D Trans F)
    extends DexService[F, Stream] {

    def getUnspentSellOrders(
      tokenId: TokenId,
      paging: Paging
    ): Stream[F, DexSellOrderInfo] =
      (for {
        output <- outputRepo.streamUnspentByErgoTreeTemplateHashAndTokenId(
                    dex.sellContractTemplateHash,
                    tokenId,
                    paging.offset,
                    paging.limit
                  )
        tokenPrice <- dex
                        .getTokenPriceFromSellOrderTree(output.output.ergoTree)
                        .asStream
        assets <- assetRepo.getAllByBoxId(output.output.boxId).asStream
      } yield DexSellOrderInfo(output, tokenPrice, assets)) ||> trans.xas

    def getUnspentBuyOrders(
      tokenId: TokenId,
      paging: Paging
    ): Stream[F, DexBuyOrderInfo] =
      (for {
        eOut <- outputRepo.streamUnspentByErgoTreeTemplateHashAndTokenId(
                  dex.buyContractTemplateHash,
                  tokenId,
                  paging.offset,
                  paging.limit
                )
        (tokenId, tokenAmount) <- dex
                                    .getTokenInfoFromBuyOrderTree(eOut.output.ergoTree)
                                    .asStream
        assets <- assetRepo.getAllByBoxId(eOut.output.boxId).asStream
      } yield DexBuyOrderInfo(eOut, tokenId, tokenAmount, assets)) ||> trans.xas
  }
}
