package org.ergoplatform.explorer.http.api.v0.services

import cats.~>
import fs2.Stream
import org.ergoplatform.explorer.db.repositories.{AssetRepo, OutputRepo, TransactionRepo}
import org.ergoplatform.explorer.http.api.models.Paging
import org.ergoplatform.explorer.http.api.v0.models.{AddressInfo, TransactionInfo}
import org.ergoplatform.explorer.{Address, TokenId}

/** A service providing an access to the addresses data.
  */
trait AddressesService[F[_], S[_[_], _]] {

  /** Get summary info for the given `address`.
    */
  def getAddressInfo(address: Address): F[Option[AddressInfo]]

  /** Get all transactions related to a given `address`.
    */
  def getTxsInfoByAddress(address: Address, paging: Paging): S[F, TransactionInfo]

  /** Get all addresses holding an asset with a given `assetId`.
    */
  def getAssetHoldersAddresses(tokenId: TokenId, paging: Paging): S[F, Address]
}

object AddressesService {

  final private class Live[F[_], D[_]](
    transactionRepo: TransactionRepo[D, Stream],
    outputRepo: OutputRepo[D, Stream],
    assetRepo: AssetRepo[D, Stream]
  )(xa: D ~> F)
    extends AddressesService[F, Stream] {

    def getAddressInfo(address: Address): F[Option[AddressInfo]] = ???

    def getTxsInfoByAddress(
      address: Address,
      paging: Paging
    ): Stream[F, TransactionInfo] = ???

    def getAssetHoldersAddresses(tokenId: TokenId, paging: Paging): Stream[F, Address] =
      ???
  }
}
