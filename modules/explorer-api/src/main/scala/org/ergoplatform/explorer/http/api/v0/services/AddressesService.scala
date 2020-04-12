package org.ergoplatform.explorer.http.api.v0.services

import cats.Monad
import cats.effect.Sync
import cats.instances.option._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.list._
import cats.syntax.traverse._
import cats.syntax.apply._
import fs2.Stream
import mouse.anyf._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.Err.RefinementFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.AddressDecodingFailed
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.{Asset, UAsset}
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.Paging
import org.ergoplatform.explorer.http.api.v0.models.{AddressInfo, AssetInfo}
import org.ergoplatform.explorer.protocol.utils
import org.ergoplatform.explorer.{Address, CRaise, TokenId}

/** A service providing an access to the addresses data.
  */
trait AddressesService[F[_], S[_[_], _]] {

  /** Get summary info for the given `address`.
    */
  def getAddressInfo(address: Address): F[AddressInfo]

  /** Get all addresses holding an asset with a given `assetId`.
    */
  def getAssetHoldersAddresses(tokenId: TokenId, paging: Paging): S[F, Address]

  /** Get all addresses matching the given `query`.
    */
  def getAllLike(query: String): F[List[Address]]
}

object AddressesService {

  def apply[
    F[_]: Sync,
    D[_]: CRaise[*[_], AddressDecodingFailed]: CRaise[*[_], RefinementFailed]: Monad: LiftConnectionIO
  ](trans: D Trans F)(implicit e: ErgoAddressEncoder): F[AddressesService[F, Stream]] =
    (OutputRepo[F, D], UOutputRepo[F, D], AssetRepo[F, D], UAssetRepo[F, D])
      .mapN(new Live(_, _, _, _)(trans))

  final private class Live[
    F[_],
    D[_]: CRaise[*[_], AddressDecodingFailed]: CRaise[*[_], RefinementFailed]: Monad
  ](
    outputRepo: OutputRepo[D, Stream],
    uOutputRepo: UOutputRepo[D, Stream],
    assetRepo: AssetRepo[D, Stream],
    uAssetRepo: UAssetRepo[D]
  )(trans: D Trans F)(implicit e: ErgoAddressEncoder)
    extends AddressesService[F, Stream] {

    def getAddressInfo(address: Address): F[AddressInfo] =
      (for {
        ergoTree      <- utils.addressToErgoTreeHex(address)
        outs          <- outputRepo.getAllMainByErgoTree(ergoTree)
        unspentOutIds <- outputRepo.getAllMainUnspentIdsByErgoTree(ergoTree)
        balance       <- outputRepo.sumOfAllMainUnspentByErgoTree(ergoTree)
        assets <- unspentOutIds.toNel
                   .traverse(assetRepo.getAllByBoxIds)
                   .map(_.toList.flatten)
        unspentOffChainOuts <- uOutputRepo.getAllUnspentByErgoTree(ergoTree)
        offChainAssets <- unspentOffChainOuts
                           .map(_.boxId)
                           .toNel
                           .traverse(uAssetRepo.getAllByBoxIds)
                           .map(_.toList.flatten)
      } yield {
        val txsQty          = outs.map(_.output.txId).distinct.size
        val offChainBalance = unspentOffChainOuts.map(_.value).sum
        val totalBalance    = balance + offChainBalance
        val totalReceived   = outs.map(o => BigDecimal(o.output.value)).sum
        val tokensBalance = assets.foldLeft(Map.empty[TokenId, Long]) {
          case (acc, Asset(assetId, _, _, assetAmt)) =>
            acc.updated(assetId, acc.getOrElse(assetId, 0L) + assetAmt)
        }
        val tokensBalanceInfo = tokensBalance.map { case (id, amt) => AssetInfo(id, amt) }.toList
        val totalTokensBalance = offChainAssets.foldLeft(tokensBalance) {
          case (acc, UAsset(assetId, _, assetAmt)) =>
            acc.updated(assetId, acc.getOrElse(assetId, 0L) + assetAmt)
        }
        val totalTokensBalanceInfo = totalTokensBalance.map {
          case (id, amt) => AssetInfo(id, amt)
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
  }
}
