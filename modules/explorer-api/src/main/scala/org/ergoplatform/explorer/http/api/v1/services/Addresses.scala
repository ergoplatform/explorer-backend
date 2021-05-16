package org.ergoplatform.explorer.http.api.v1.services

import cats.Monad
import cats.effect.Sync
import fs2.Stream
import mouse.anyf._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.Address
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.v1.models.{Balance, TokenAmount, TotalBalance, TransactionInfo}
import org.ergoplatform.explorer.protocol.sigma
import tofu.syntax.monadic._

trait Addresses[F[_]] {

  def confirmedBalanceOf(address: Address, minConfirmations: Int): F[Balance]

  def totalBalanceOf(address: Address): F[TotalBalance]
}

object Addresses {

  def apply[F[_]: Sync, D[_]: Monad: LiftConnectionIO](trans: D Trans F)(implicit
    e: ErgoAddressEncoder
  ): F[Addresses[F]] =
    (HeaderRepo[F, D], OutputRepo[F, D], AssetRepo[F, D], UOutputRepo[F, D], UAssetRepo[F, D])
      .mapN(new Live(_, _, _, _, _)(trans))

  final class Live[F[_], D[_]: Monad](
    headerRepo: HeaderRepo[D],
    outputRepo: OutputRepo[D, Stream],
    assetRepo: AssetRepo[D, Stream],
    uOutputRepo: UOutputRepo[D, Stream],
    uAssetRepo: UAssetRepo[D]
  )(trans: D Trans F)(implicit e: ErgoAddressEncoder)
    extends Addresses[F] {

    def confirmedBalanceOf(address: Address, minConfirmations: Int): F[Balance] =
      (for {
        height <- if (minConfirmations > 0) headerRepo.getBestHeight else Int.MaxValue.pure[D]
        maxHeight = height - minConfirmations
        tree      = sigma.addressToErgoTreeHex(address)
        balance <- outputRepo.sumUnspentByErgoTree(tree, maxHeight)
        assets  <- assetRepo.aggregateUnspentByErgoTree(tree, maxHeight)
      } yield Balance(balance, assets.map(TokenAmount(_)))) ||> trans.xa

    def totalBalanceOf(address: Address): F[TotalBalance] = {
      val tree = sigma.addressToErgoTreeHex(address)
      (for {
        balance         <- outputRepo.sumUnspentByErgoTree(tree, Int.MaxValue)
        assets          <- assetRepo.aggregateUnspentByErgoTree(tree, Int.MaxValue)
        offChainBalance <- uOutputRepo.sumUnspentByErgoTree(tree)
        offChainAssets  <- uAssetRepo.aggregateUnspentByErgoTree(tree)
      } yield TotalBalance(
        confirmed   = Balance(balance, assets.map(TokenAmount(_))),
        unconfirmed = Balance(offChainBalance, offChainAssets.map(TokenAmount(_)))
      )) ||> trans.xa
    }
  }
}
