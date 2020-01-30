package org.ergoplatform.explorer.http.api.v0.services

import cats.syntax.list._
import cats.{~>, Monad}
import fs2.Stream
import org.ergoplatform.explorer.Err.RequestProcessingErr.InconsistentDbData
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.v0.models.OutputInfo
import org.ergoplatform.explorer.syntax.stream._
import tofu.Raise.ContravariantRaise
import tofu.syntax.raise._

/** A service providing an access to the DEX data.
  */
trait DexService[F[_], S[_[_], _]] {

  def getUnspentSellOrders(tokenId: TokenId): S[F, OutputInfo]

  def getUnspentBuyOrders(tokenId: TokenId): S[F, OutputInfo]
}

object DexService {

  def apply[
    F[_],
    D[_]: LiftConnectionIO: ContravariantRaise[*[_], InconsistentDbData]: Monad
  ](
    xa: D ~> F
  ): DexService[F, Stream] =
    new Live(AssetRepo[D])(xa)

  final private class Live[
    F[_],
    D[_]: ContravariantRaise[*[_], InconsistentDbData]: Monad
  ](
    assetRepo: AssetRepo[D, Stream]
  )(xa: D ~> F)
    extends DexService[F, Stream] {

    override def getUnspentSellOrders(tokenId: TokenId): Stream[F, OutputInfo] = ???
    override def getUnspentBuyOrders(tokenId: TokenId): Stream[F, OutputInfo]  = ???

  }
}
