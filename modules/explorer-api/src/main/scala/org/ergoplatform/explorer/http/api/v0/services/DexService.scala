package org.ergoplatform.explorer.http.api.v0.services

import cats.{~>, Monad}
import fs2.Stream
import org.ergoplatform.explorer.Err.DexErr
import org.ergoplatform.explorer.Err.RequestProcessingErr.{
  Base16DecodingFailed,
  ErgoTreeDeserializationFailed
}
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.v0.models.{DexBuyOrderInfo, DexSellOrderInfo}
import org.ergoplatform.explorer.services.DexCoreService
import org.ergoplatform.explorer.syntax.stream._
import tofu.Raise.ContravariantRaise

/** A service providing an access to the DEX data.
  */
trait DexService[F[_], S[_[_], _]] {

  def getUnspentSellOrders(tokenId: TokenId): S[F, DexSellOrderInfo]

  def getUnspentBuyOrders(tokenId: TokenId): S[F, DexBuyOrderInfo]
}

object DexService {

  def apply[
    F[_],
    D[_]: LiftConnectionIO: Monad: ContravariantRaise[
      *[_],
      DexErr
    ]: ContravariantRaise[
      *[_],
      Base16DecodingFailed
    ]: ContravariantRaise[
      *[_],
      ErgoTreeDeserializationFailed
    ]
  ](
    xa: D ~> F
  ): DexService[F, Stream] =
    new Live(AssetRepo[D], DexCoreService[D])(xa)

  final private class Live[
    F[_],
    D[_]: Monad
  ](
    assetRepo: AssetRepo[D, Stream],
    dexCoreService: DexCoreService[D, Stream]
  )(xa: D ~> F)
    extends DexService[F, Stream] {

    override def getUnspentSellOrders(tokenId: TokenId): Stream[F, DexSellOrderInfo] =
      (
        for {
          sellOrderOut <- dexCoreService
                           .getAllMainUnspentSellOrderByTokenId(
                             tokenId
                           )
          assets <- assetRepo.getAllByBoxId(sellOrderOut.extOutput.output.boxId).asStream
        } yield DexSellOrderInfo(sellOrderOut, assets)
      ).translate(xa)

    override def getUnspentBuyOrders(tokenId: TokenId): Stream[F, DexBuyOrderInfo] =
      (
        for {
          buyOrder <- dexCoreService
                       .getAllMainUnspentBuyOrderByTokenId(tokenId)
          assets <- assetRepo.getAllByBoxId(buyOrder.extOutput.output.boxId).asStream
        } yield DexBuyOrderInfo(buyOrder, assets)
      ).translate(xa)

  }

}
