package org.ergoplatform.explorer.db.repositories

import fs2.Stream
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer.db.models.schema.ctx._
import org.ergoplatform.explorer.{HexString, TokenId}
import org.ergoplatform.explorer.db.quillCodecs._

/** [[ExtendedOutput]] for DEX sell/buy orders data access operations.
  */
trait DexOrdersRepo[D[_], S[_[_], _]] {

  /** Get all unspent main-chain DEX sell orders
    */
  def getAllMainUnspentSellOrderByTokenId(
    tokenId: TokenId,
    ergoTreeTemplate: HexString
  ): S[D, ExtendedOutput]

  /** Get all unspent main-chain DEX buy orders
    */
  def getAllMainUnspentBuyOrderByTokenId(
    tokenId: TokenId,
    ergoTreeTemplate: HexString
  ): S[D, ExtendedOutput]
}

object DexOrdersRepo {

  def apply[D[_]: LiftConnectionIO]: DexOrdersRepo[D, Stream] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO] extends DexOrdersRepo[D, Stream] {

    import org.ergoplatform.explorer.db.queries.{DexOrdersQuerySet => QS}

    private val liftK = LiftConnectionIO[D].liftConnectionIOK

    override def getAllMainUnspentSellOrderByTokenId(
      tokenId: TokenId,
      ergoTreeTemplate: HexString
    ): Stream[D, ExtendedOutput] =
      stream(
        QS.getMainUnspentSellOrderByTokenId(tokenId, ergoTreeTemplate, 0, Int.MaxValue)
      ).translate(liftK)

    /** Get all unspent main-chain DEX buy orders
      */
    override def getAllMainUnspentBuyOrderByTokenId(
      tokenId: TokenId,
      ergoTreeTemplate: HexString
    ): Stream[D, ExtendedOutput] =
      stream(
        QS.getMainUnspentBuyOrderByTokenId(tokenId, ergoTreeTemplate, 0, Int.MaxValue)
      ).translate(liftK)

  }
}
