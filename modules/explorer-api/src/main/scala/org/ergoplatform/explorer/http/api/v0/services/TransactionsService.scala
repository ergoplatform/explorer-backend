package org.ergoplatform.explorer.http.api.v0.services

import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.list._
import cats.syntax.traverse._
import cats.{~>, Monad}
import fs2.{Chunk, Pipe, Stream}
import mouse.anyf._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.Transaction
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.Paging
import org.ergoplatform.explorer.http.api.v0.models.{TransactionInfo, UTransactionInfo}
import org.ergoplatform.explorer.syntax.stream._
import org.ergoplatform.explorer.{Address, TxId}

/** A service providing an access to the transactions data.
  */
trait TransactionsService[F[_], S[_[_], _]] {

  /** Get transaction info by `id`.
    */
  def getTxInfo(id: TxId): F[Option[TransactionInfo]]

  /** Get unconfirmed transaction info by `id`.
    */
  def getUnconfirmedTxInfo(id: TxId): F[Option[UTransactionInfo]]

  /** Get transactions related to a given `address`.
    */
  def getTxsInfoByAddress(address: Address, paging: Paging): S[F, TransactionInfo]

  /** Get all transactions appeared in the blockchain after the given `height`.
    */
  def getTxsSince(height: Int, paging: Paging): S[F, TransactionInfo]
}

object TransactionsService {

  def apply[F[_], D[_]: Monad: LiftConnectionIO](
    xa: D ~> F
  )(implicit e: ErgoAddressEncoder): TransactionsService[F, Stream] =
    new Live(
      HeaderRepo[D],
      TransactionRepo[D],
      UTransactionRepo[D],
      InputRepo[D],
      UInputRepo[D],
      OutputRepo[D],
      UOutputRepo[D],
      AssetRepo[D],
      UAssetRepo[D]
    )(xa)

  final private class Live[F[_], D[_]: Monad](
    headerRepo: HeaderRepo[D],
    transactionRepo: TransactionRepo[D, Stream],
    uTransactionRepo: UTransactionRepo[D, Stream],
    inputRepo: InputRepo[D],
    uInputRepo: UInputRepo[D, Stream],
    outputRepo: OutputRepo[D, Stream],
    uOutputRepo: UOutputRepo[D, Stream],
    assetRepo: AssetRepo[D, Stream],
    uAssetRepo: UAssetRepo[D]
  )(xa: D ~> F)(implicit e: ErgoAddressEncoder)
    extends TransactionsService[F, Stream] {

    def getTxInfo(id: TxId): F[Option[TransactionInfo]] =
      (for {
        txOpt <- transactionRepo.getMain(id)
        ins   <- txOpt.toList.flatTraverse(tx => inputRepo.getAllByTxId(tx.id))
        outs  <- txOpt.toList.flatTraverse(tx => outputRepo.getAllByTxId(tx.id))
        boxIdsNel = outs.map(_.output.boxId).toNel
        assets     <- boxIdsNel.toList.flatTraverse(assetRepo.getAllByBoxIds)
        bestHeight <- headerRepo.getBestHeight
        txInfo = txOpt.map(tx =>
          TransactionInfo(tx, bestHeight - tx.inclusionHeight, ins, outs, assets)
        )
      } yield txInfo) ||> xa

    def getUnconfirmedTxInfo(id: TxId): F[Option[UTransactionInfo]] =
      (for {
        txOpt <- uTransactionRepo.get(id)
        ins   <- txOpt.toList.flatTraverse(tx => uInputRepo.getAllByTxId(tx.id))
        outs  <- txOpt.toList.flatTraverse(tx => uOutputRepo.getAllByTxId(tx.id))
        boxIdsNel = outs.map(_.boxId).toNel
        assets <- boxIdsNel.toList.flatTraverse(uAssetRepo.getAllByBoxIds)
        txInfo = txOpt.map(UTransactionInfo(_, ins, outs, assets))
      } yield txInfo) ||> xa

    def getTxsInfoByAddress(
      address: Address,
      paging: Paging
    ): Stream[F, TransactionInfo] =
      transactionRepo
        .getRelatedToAddress(address, paging.offset, paging.limit)
        .chunkN(100)
        .through(assembleInfo)
        .translate(xa)

    def getTxsSince(height: Int, paging: Paging): Stream[F, TransactionInfo] =
      transactionRepo
        .getMainSince(height, paging.offset, paging.limit)
        .chunkN(100)
        .through(assembleInfo)
        .translate(xa)

    private def assembleInfo: Pipe[D, Chunk[Transaction], TransactionInfo] =
      _.flatMap { txChunk =>
        for {
          txIdsNel   <- txChunk.map(_.id).toList.toNel.toStream.covary[D]
          ins        <- inputRepo.getAllByTxIds(txIdsNel).asStream
          outs       <- outputRepo.getAllByTxIds(txIdsNel).asStream
          boxIdsNel  <- outs.map(_.output.boxId).toNel.toStream.covary[D]
          assets     <- assetRepo.getAllByBoxIds(boxIdsNel).asStream
          bestHeight <- headerRepo.getBestHeight.asStream
          txsWithHeights = txChunk
            .map(tx => tx -> (bestHeight - tx.inclusionHeight))
            .toList
          txInfo <- Stream
                     .emits(TransactionInfo.batch(txsWithHeights, ins, outs, assets))
                     .covary[D]
        } yield txInfo
      }
  }
}
