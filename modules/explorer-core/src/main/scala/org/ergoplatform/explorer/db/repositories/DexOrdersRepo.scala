package org.ergoplatform.explorer.db.repositories

import fs2.Stream
import org.ergoplatform.explorer.db.DexContracts
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.aggregates.{
  DexBuyOrderOutput,
  DexSellOrderOutput
}
import org.ergoplatform.explorer.{HexString, TokenId}

/** [[ExtendedOutput]] for DEX sell/buy orders data access operations.
  */
trait DexOrdersRepo[D[_], S[_[_], _]] {

  /** Get all unspent main-chain DEX sell orders
    */
  def getAllMainUnspentSellOrderByTokenId(
    tokenId: TokenId
  ): S[D, DexSellOrderOutput]

  /** Get all unspent main-chain DEX buy orders
    */
  def getAllMainUnspentBuyOrderByTokenId(
    tokenId: TokenId
  ): S[D, DexBuyOrderOutput]
}

object DexOrdersRepo {

  def apply[D[_]: LiftConnectionIO]: DexOrdersRepo[D, Stream] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO] extends DexOrdersRepo[D, Stream] {

    import org.ergoplatform.explorer.db.queries.{DexOrdersQuerySet => QS}

    private val liftK = LiftConnectionIO[D].liftConnectionIOK

    override def getAllMainUnspentSellOrderByTokenId(
      tokenId: TokenId
    ): Stream[D, DexSellOrderOutput] =
      QS.getMainUnspentSellOrderByTokenId(
          tokenId,
          DexContracts.sellContractTemplate,
          0,
          Int.MaxValue
        )
        .stream
        .map(eOut =>
          DexSellOrderOutput(
            eOut,
            DexContracts.getTokenPriceFromSellOrderTree(eOut.output.ergoTree).get
          )
        )
        .translate(liftK)

    /** Get all unspent main-chain DEX buy orders
      */
    override def getAllMainUnspentBuyOrderByTokenId(
      tokenId: TokenId
    ): Stream[D, DexBuyOrderOutput] =
      QS.getMainUnspentBuyOrderByTokenId(
          tokenId,
          DexContracts.buyContractTemplate,
          0,
          Int.MaxValue
        )
        .stream
        .map(eOut =>
          DexBuyOrderOutput(
            eOut,
            DexContracts.getTokenInfoFromBuyOrderTree(eOut.output.ergoTree).get
          )
        )
        .translate(liftK)

  }

}
