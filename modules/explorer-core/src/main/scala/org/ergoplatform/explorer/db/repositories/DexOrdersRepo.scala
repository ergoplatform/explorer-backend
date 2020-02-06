package org.ergoplatform.explorer.db.repositories

import cats.Monad
import fs2.Stream
import org.ergoplatform.explorer.Err.DexErr.{
  DexBuyOrderAttributesFailed,
  DexSellOrderAttributesFailed
}
import org.ergoplatform.explorer.Err.RequestProcessingErr.{
  Base16DecodingFailed,
  ErgoTreeDeserializationFailed
}
import org.ergoplatform.explorer.db.DexContracts
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.aggregates.{
  DexBuyOrderOutput,
  DexSellOrderOutput
}
import org.ergoplatform.explorer.{HexString, TokenId}
import org.ergoplatform.explorer.syntax.stream._
import tofu.Raise.ContravariantRaise

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

  def apply[D[_]: LiftConnectionIO: ContravariantRaise[
    *[_],
    DexSellOrderAttributesFailed
  ]: ContravariantRaise[
    *[_],
    Base16DecodingFailed
  ]: ContravariantRaise[
    *[_],
    ErgoTreeDeserializationFailed
  ]: ContravariantRaise[
    *[_],
    DexBuyOrderAttributesFailed
  ]: Monad]: DexOrdersRepo[D, Stream] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO: ContravariantRaise[
    *[_],
    DexSellOrderAttributesFailed
  ]: ContravariantRaise[
    *[_],
    Base16DecodingFailed
  ]: ContravariantRaise[
    *[_],
    ErgoTreeDeserializationFailed
  ]: ContravariantRaise[
    *[_],
    DexBuyOrderAttributesFailed
  ]: Monad]
    extends DexOrdersRepo[D, Stream] {

    import org.ergoplatform.explorer.db.queries.{DexOrdersQuerySet => QS}

    private val liftK = LiftConnectionIO[D].liftConnectionIOK

    override def getAllMainUnspentSellOrderByTokenId(
      tokenId: TokenId
    ): Stream[D, DexSellOrderOutput] =
      for {
        eOut <- QS
                 .getMainUnspentSellOrderByTokenId(
                   tokenId,
                   DexContracts.sellContractTemplate,
                   0,
                   Int.MaxValue
                 )
                 .stream
                 .translate(liftK)
        tokenPrice <- DexContracts
                       .getTokenPriceFromSellOrderTree(eOut.output.ergoTree)
                       .asStream
      } yield DexSellOrderOutput(eOut, tokenPrice)

    override def getAllMainUnspentBuyOrderByTokenId(
      tokenId: TokenId
    ): Stream[D, DexBuyOrderOutput] =
      for {
        eOut <- QS
                 .getMainUnspentBuyOrderByTokenId(
                   tokenId,
                   DexContracts.buyContractTemplate,
                   0,
                   Int.MaxValue
                 )
                 .stream
                 .translate(liftK)
        tokenInfo <- DexContracts
                      .getTokenInfoFromBuyOrderTree(eOut.output.ergoTree)
                      .asStream
      } yield DexBuyOrderOutput(eOut, tokenInfo)

  }

}
