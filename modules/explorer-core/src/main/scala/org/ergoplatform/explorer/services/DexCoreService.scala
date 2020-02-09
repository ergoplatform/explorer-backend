package org.ergoplatform.explorer.services

import cats.Monad
import fs2.Stream
import org.ergoplatform.explorer.Err.DexErr
import org.ergoplatform.explorer.Err.RequestProcessingErr.{
  Base16DecodingFailed,
  ErgoTreeDeserializationFailed
}
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.db.DexContracts
import org.ergoplatform.explorer.db.models.aggregates.{
  DexBuyOrderOutput,
  DexSellOrderOutput
}
import org.ergoplatform.explorer.db.repositories.OutputRepo
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.syntax.stream._
import tofu.Raise.ContravariantRaise

/** A service providing an access to the DEX contracts
  */
trait DexCoreService[F[_], S[_[_], _]] {

  /** Get all unspent main-chain DEX sell orders
    */
  def getAllMainUnspentSellOrderByTokenId(
    tokenId: TokenId
  ): S[F, DexSellOrderOutput]

  /** Get all unspent main-chain DEX buy orders
    */
  def getAllMainUnspentBuyOrderByTokenId(
    tokenId: TokenId
  ): S[F, DexBuyOrderOutput]

}

object DexCoreService {

  def apply[F[_]: LiftConnectionIO: Monad: ContravariantRaise[
    *[_],
    DexErr
  ]: ContravariantRaise[
    *[_],
    Base16DecodingFailed
  ]: ContravariantRaise[*[_], ErgoTreeDeserializationFailed]]: DexCoreService[F, Stream] =
    new Live[F](OutputRepo[F])

  def apply[F[_]: Monad: ContravariantRaise[
    *[_],
    DexErr
  ]: ContravariantRaise[
    *[_],
    Base16DecodingFailed
  ]: ContravariantRaise[*[_], ErgoTreeDeserializationFailed]](
    outputRepo: OutputRepo[F, Stream]
  ): DexCoreService[F, Stream] =
    new Live[F](outputRepo)

  final private class Live[F[_]: Monad: ContravariantRaise[
    *[_],
    DexErr
  ]: ContravariantRaise[
    *[_],
    Base16DecodingFailed
  ]: ContravariantRaise[
    *[_],
    ErgoTreeDeserializationFailed
  ]](outputRepo: OutputRepo[F, Stream])
    extends DexCoreService[F, Stream] {

    /** Get all unspent main-chain DEX sell orders
      */
    override def getAllMainUnspentSellOrderByTokenId(
      tokenId: TokenId
    ): Stream[F, DexSellOrderOutput] =
      for {
        eOut <- outputRepo.getAllMainUnspentSellOrderByTokenId(tokenId)
        tokenPrice <- DexContracts
                       .getTokenPriceFromSellOrderTree(eOut.output.ergoTree)
                       .asStream
      } yield DexSellOrderOutput(eOut, tokenPrice)

    /** Get all unspent main-chain DEX buy orders
      */
    override def getAllMainUnspentBuyOrderByTokenId(
      tokenId: TokenId
    ): Stream[F, DexBuyOrderOutput] =
      for {
        eOut <- outputRepo.getAllMainUnspentBuyOrderByTokenId(tokenId)
        tokenInfo <- DexContracts
                      .getTokenInfoFromBuyOrderTree(eOut.output.ergoTree)
                      .asStream
      } yield DexBuyOrderOutput(eOut, tokenInfo)

  }
}
