package org.ergoplatform.explorer.http.api.v0.services

import cats.{~>, Monad}
import fs2.Stream
import org.ergoplatform.explorer.Err.RefinementFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.ContractParsingErr.Base16DecodingFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.{
  DexErr,
  ErgoTreeSerializationErr
}
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.Paging
import org.ergoplatform.explorer.http.api.v0.models.{DexBuyOrderInfo, DexSellOrderInfo}
import org.ergoplatform.explorer.protocol.dex
import org.ergoplatform.explorer.syntax.stream._
import tofu.Raise.ContravariantRaise

/** A service providing an access to the DEX data.
  */
trait DexService[F[_], S[_[_], _]] {

  def getUnspentSellOrders(tokenId: TokenId, paging: Paging): S[F, DexSellOrderInfo]

  def getUnspentBuyOrders(tokenId: TokenId, paging: Paging): S[F, DexBuyOrderInfo]
}

object DexService {

  def apply[
    F[_],
    D[_]: LiftConnectionIO: Monad: ContravariantRaise[*[_], DexErr]: ContravariantRaise[*[
      _
    ], ErgoTreeSerializationErr]: ContravariantRaise[*[
      _
    ], Base16DecodingFailed]: ContravariantRaise[
      *[_],
      RefinementFailed
    ]
  ](xa: D ~> F): DexService[F, Stream] =
    new Live(AssetRepo[D], OutputRepo[D])(xa)

  final private class Live[
    F[_],
    D[_]: Monad: ContravariantRaise[*[_], DexErr]: ContravariantRaise[*[_], ErgoTreeSerializationErr]: ContravariantRaise[
      *[
        _
      ],
      Base16DecodingFailed
    ]: ContravariantRaise[
      *[_],
      RefinementFailed
    ]
  ](
    assetRepo: AssetRepo[D, Stream],
    outputRepo: OutputRepo[D, Stream]
  )(xa: D ~> F)
    extends DexService[F, Stream] {

    def getUnspentSellOrders(
      tokenId: TokenId,
      paging: Paging
    ): Stream[F, DexSellOrderInfo] =
      (for {
        sellOrderTemplate <- dex.sellContractTemplate.asStream
        output <- outputRepo.getAllMainUnspentSellOrderByTokenId(
                   tokenId,
                   sellOrderTemplate,
                   paging.offset,
                   paging.limit
                 )
        tokenPrice <- dex
                       .getTokenPriceFromSellOrderTree(output.output.ergoTree)
                       .asStream
        assets <- assetRepo.getAllByBoxId(output.output.boxId).asStream
      } yield DexSellOrderInfo(output, tokenPrice, assets)).translate(xa)

    def getUnspentBuyOrders(
      tokenId: TokenId,
      paging: Paging
    ): Stream[F, DexBuyOrderInfo] =
      (for {
        buyOrderTemplate <- dex.buyContractTemplate.asStream
        eOut <- outputRepo.getAllMainUnspentBuyOrderByTokenId(
                 tokenId,
                 buyOrderTemplate,
                 paging.offset,
                 paging.limit
               )
        (tokenId, tokenAmount) <- dex
                                   .getTokenInfoFromBuyOrderTree(eOut.output.ergoTree)
                                   .asStream
        assets <- assetRepo.getAllByBoxId(eOut.output.boxId).asStream
      } yield DexBuyOrderInfo(eOut, tokenId, tokenAmount, assets)).translate(xa)
  }
}
