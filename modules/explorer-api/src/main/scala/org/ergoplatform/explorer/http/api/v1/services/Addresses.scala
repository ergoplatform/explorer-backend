package org.ergoplatform.explorer.http.api.v1.services

import cats.{Monad, Parallel}
import cats.effect.Sync
import fs2.Stream
import mouse.anyf._
import cats.syntax.parallel._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.{Address, ErgoTree, HexString}
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.v1.models.{AddressInfo, Balance, TokenAmount, TotalBalance}
import org.ergoplatform.explorer.protocol.sigma
import tofu.syntax.monadic._

trait Addresses[F[_]] {

  def confirmedBalanceOf(address: Address, minConfirmations: Int): F[Balance]

  def totalBalanceOf(address: Address): F[TotalBalance]

  def addressInfoOf(batch: List[Address], hasUnconfirmedTxs: ErgoTree => F[Boolean]): F[Map[Address, AddressInfo]]
}

object Addresses {

  def apply[F[_]: Sync: Monad: Parallel, D[_]: Monad: LiftConnectionIO](trans: D Trans F)(implicit
    e: ErgoAddressEncoder
  ): F[Addresses[F]] =
    (HeaderRepo[F, D], OutputRepo[F, D], AssetRepo[F, D], UOutputRepo[F, D], UAssetRepo[F, D])
      .mapN(new Live(_, _, _, _, _)(trans))

  final class Live[F[_]: Monad: Parallel, D[_]: Monad](
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

    private def hasBeenUsedByErgoTree(ergoTree: HexString): F[Boolean] =
      outputRepo.nodeOutputCount(ergoTree).map(_ > 0) ||> trans.xa

    private def addressInfoOf(
      address: Address,
      hasUnconfirmedTxs: ErgoTree => F[Boolean]
    ): F[(Address, AddressInfo)] = {
      val tree = sigma.addressToErgoTreeNewtype(address)
      for {
        balance <- confirmedBalanceOf(address, 0)
        hUTxs   <- hasUnconfirmedTxs(tree)
        used    <- hasBeenUsedByErgoTree(tree.value)
      } yield (address, AddressInfo(address = address, hasUnconfirmedTxs = hUTxs, used = used, balance))
    }

    def addressInfoOf(batch: List[Address], hasUnconfirmedTxs: ErgoTree => F[Boolean]): F[Map[Address, AddressInfo]] =
      batch.distinct
        .map(addressInfoOf(_, hasUnconfirmedTxs))
        .grouped(10)
        .map(_.parSequence)
        .toList
        .parSequence
        .map(_.flatten)
        .map(_.foldLeft(Map[Address, AddressInfo]()) { case (m, t) => m + t })
  }
}
