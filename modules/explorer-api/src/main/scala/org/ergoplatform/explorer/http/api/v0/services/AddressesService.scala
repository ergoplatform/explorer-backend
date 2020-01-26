package org.ergoplatform.explorer.http.api.v0.services

import cats.data.NonEmptyList
import cats.instances.option._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.list._
import cats.syntax.traverse._
import cats.{~>, Monad}
import mouse.anyf._
import fs2.Stream
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.db.models.{Asset, UAsset}
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.Paging
import org.ergoplatform.explorer.http.api.v0.models.{
  AddressInfo,
  AssetInfo,
  TransactionInfo
}
import org.ergoplatform.explorer.protocol.utils
import org.ergoplatform.explorer.{Address, BoxId, Err, TokenId, TxId}
import org.ergoplatform.explorer.syntax.stream._
import tofu.Raise.ContravariantRaise

/** A service providing an access to the addresses data.
  */
trait AddressesService[F[_], S[_[_], _]] {

  /** Get summary info for the given `address`.
    */
  def getAddressInfo(address: Address): F[AddressInfo]

  /** Get all transactions related to a given `address`.
    */
  def getTxsInfoByAddress(address: Address, paging: Paging): S[F, TransactionInfo]

  /** Get all addresses holding an asset with a given `assetId`.
    */
  def getAssetHoldersAddresses(tokenId: TokenId, paging: Paging): S[F, Address]
}

object AddressesService {

  final private class Live[F[_], D[_]: ContravariantRaise[*[_], Err]: Monad](
    headerRepo: HeaderRepo[D],
    transactionRepo: TransactionRepo[D, Stream],
    inputRepo: InputRepo[D],
    outputRepo: OutputRepo[D, Stream],
    uOutputRepo: UOutputRepo[D, Stream],
    assetRepo: AssetRepo[D, Stream],
    uAssetRepo: UAssetRepo[D]
  )(xa: D ~> F)(implicit e: ErgoAddressEncoder)
    extends AddressesService[F, Stream] {

    def getAddressInfo(address: Address): F[AddressInfo] =
      (for {
        ergoTree    <- utils.addressToErgoTreeHex(address)
        outs        <- outputRepo.getAllByErgoTree(ergoTree)
        unspentOuts <- outputRepo.getAllMainUnspentByErgoTree(ergoTree)
        assets <- unspentOuts
                   .map(_.output.boxId)
                   .toNel
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
        val balance         = unspentOuts.map(_.output.value).sum
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
      }) ||> xa

    def getTxsInfoByAddress(
      address: Address,
      paging: Paging
    ): Stream[F, TransactionInfo] =
      (for {
        txChunk <- transactionRepo
                    .getRelatedToAddress(address, paging.offset, paging.limit)
                    .chunkN(100)
        txIdsNel   <- txChunk.map(_.id).toList.toNel.toStream.covary[D]
        ins        <- inputRepo.getAllByTxIds(txIdsNel).asStream
        outs       <- outputRepo.getAllByTxIds(txIdsNel).asStream
        boxIdsNel  <- outs.map(_.output.boxId).toNel.toStream.covary[D]
        assets     <- assetRepo.getAllByBoxIds(boxIdsNel).asStream
        bestHeight <- headerRepo.getBestHeight.asStream
        txsWithHeights = txChunk.map(tx => tx -> (bestHeight - tx.inclusionHeight)).toList
        result <- Stream
                   .emits(TransactionInfo.batch(txsWithHeights, ins, outs, assets))
                   .covary[D]
      } yield result).translate(xa)

    def getAssetHoldersAddresses(tokenId: TokenId, paging: Paging): Stream[F, Address] =
      ???
  }
}
