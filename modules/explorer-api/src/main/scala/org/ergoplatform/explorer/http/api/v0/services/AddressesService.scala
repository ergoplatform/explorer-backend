package org.ergoplatform.explorer.http.api.v0.services

import cats.Monad
import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import mouse.anyf._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.Err.RefinementFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.AddressDecodingFailed
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, ExtendedUAsset}
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.v0.models.{AddressInfo, AssetSummary, BalanceInfo}
import org.ergoplatform.explorer.protocol.sigma
import org.ergoplatform.explorer.{Address, CRaise, TokenId}

/** A service providing an access to the addresses data.
  */
trait AddressesService[F[_], S[_[_], _]] {

  /** Get summary info for the given `address`.
    */
  def getAddressInfo(address: Address, minConfirmations: Int): F[AddressInfo]

  /** Get all addresses holding an asset with a given `assetId`.
    */
  def getAssetHoldersAddresses(tokenId: TokenId, paging: Paging): S[F, Address]

  /** Get all addresses matching the given `query`.
    */
  def getAllLike(query: String): F[List[Address]]

  /** Get balances by all addresses in the network.
    */
  def balances(paging: Paging): F[Items[BalanceInfo]]
}

object AddressesService {

  def apply[
    F[_]: Sync,
    D[_]: CRaise[*[_], AddressDecodingFailed]: CRaise[*[_], RefinementFailed]: Monad: LiftConnectionIO
  ](trans: D Trans F)(implicit e: ErgoAddressEncoder): F[AddressesService[F, Stream]] =
    (HeaderRepo[F, D], TransactionRepo[F, D], OutputRepo[F, D], UOutputRepo[F, D], AssetRepo[F, D], UAssetRepo[F, D])
      .mapN(new Live(_, _, _, _, _, _)(trans))

  private case class AssetData(amount: Long, decimals: Option[Int], name: Option[String]) {
    def addAmount(amt: Long): AssetData = copy(amount = amount + amt)
  }

  final private class Live[
    F[_],
    D[_]: CRaise[*[_], AddressDecodingFailed]: CRaise[*[_], RefinementFailed]: Monad
  ](
    headerRepo: HeaderRepo[D],
    txRepo: TransactionRepo[D, Stream],
    outputRepo: OutputRepo[D, Stream],
    uOutputRepo: UOutputRepo[D, Stream],
    assetRepo: AssetRepo[D, Stream],
    uAssetRepo: UAssetRepo[D]
  )(trans: D Trans F)(implicit e: ErgoAddressEncoder)
    extends AddressesService[F, Stream] {

    // todo: optimize: use pg aggregation
    def getAddressInfo(address: Address, minConfirmations: Int): F[AddressInfo] =
      (for {
        ergoTree <- sigma.addressToErgoTreeHex(address)
        height   <- if (minConfirmations > 0) headerRepo.getBestHeight else Int.MaxValue.pure[D]
        maxHeight = height - minConfirmations
        outs                <- outputRepo.getAllByErgoTree(ergoTree, maxHeight)
        balance             <- outputRepo.sumUnspentByErgoTree(ergoTree, maxHeight)
        assets              <- assetRepo.getAllMainUnspentByErgoTree(ergoTree)
        unspentOffChainOuts <- uOutputRepo.getAllUnspentByErgoTree(ergoTree)
        offChainAssets      <- uAssetRepo.getAllUnspentByErgoTree(ergoTree)
        txsQty              <- txRepo.countRelatedToAddress(address)
      } yield {
        val offChainBalance = unspentOffChainOuts.map(_.value).sum
        val totalBalance    = balance + offChainBalance
        val totalReceived   = outs.map(o => BigDecimal(o.output.value)).sum
        val tokensBalance =
          assets.foldLeft(Map.empty[TokenId, AssetData]) {
            case (acc, ExtendedAsset(assetId, _, _, _, assetAmt, name, decimals, _)) =>
              val init  = AssetData(0L, decimals, name)
              val asset = acc.getOrElse(assetId, init)
              acc.updated(assetId, asset addAmount assetAmt)
          }
        val tokensBalanceInfo =
          tokensBalance.map { case (id, asset) =>
            AssetSummary(id, asset.amount, asset.name, asset.decimals)
          }.toList
        val totalTokensBalance =
          offChainAssets.foldLeft(tokensBalance) {
            case (acc, ExtendedUAsset(assetId, _, _, assetAmt, name, decimals, _)) =>
              val init  = AssetData(0L, decimals, name)
              val asset = acc.getOrElse(assetId, init)
              acc.updated(assetId, asset addAmount assetAmt)
          }
        val totalTokensBalanceInfo =
          totalTokensBalance.map { case (id, asset) =>
            AssetSummary(id, asset.amount, asset.name, asset.decimals)
          }.toList
        AddressInfo(
          address,
          txsQty,
          totalReceived,
          balance,
          totalBalance,
          tokensBalanceInfo,
          totalTokensBalanceInfo
        )
      }) ||> trans.xa

    def getAssetHoldersAddresses(tokenId: TokenId, paging: Paging): Stream[F, Address] =
      assetRepo.getAllHoldingAddresses(tokenId, paging.offset, paging.limit) ||> trans.xas

    def getAllLike(query: String): F[List[Address]] =
      outputRepo.getAllLike(query) ||> trans.xa

    def balances(paging: Paging): F[Items[BalanceInfo]] =
      outputRepo
        .balanceStatsMain(0, 100) // limit balances list to first 100
        .map(_.map { case (address, balance) => BalanceInfo(address, balance) })
        .map(xs => Items(xs.slice(paging.offset, paging.offset + paging.limit), xs.size)) ||> trans.xa
  }
}
