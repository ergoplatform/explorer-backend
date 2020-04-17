package org.ergoplatform.explorer.http.api.v0.services

import cats.effect.Concurrent
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.list._
import cats.syntax.apply._
import cats.syntax.traverse._
import cats.{FlatMap, Monad}
import dev.profunktor.redis4cats.algebra.RedisCommands
import fs2.{Chunk, Pipe, Stream}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.estatico.newtype.ops._
import mouse.anyf._
import org.ergoplatform.explorer.cache.repositories.ErgoLikeTransactionRepo
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.Transaction
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.Paging
import org.ergoplatform.explorer.http.api.v0.models.{TransactionInfo, TransactionSummary, TxIdResponse, UTransactionInfo}
import org.ergoplatform.explorer.settings.UtxCacheSettings
import org.ergoplatform.explorer.syntax.stream._
import org.ergoplatform.explorer.{Address, TxId}
import org.ergoplatform.{ErgoAddressEncoder, ErgoLikeTransaction}

/** A service providing an access to the transactions data.
  */
trait TransactionsService[F[_], S[_[_], _]] {

  /** Get transaction info by `id`.
    */
  def getTxInfo(id: TxId): F[Option[TransactionSummary]]

  /** Get unconfirmed transaction info by `id`.
    */
  def getUnconfirmedTxInfo(id: TxId): F[Option[UTransactionInfo]]

  /** Get transactions related to a given `address`.
    */
  def getTxsInfoByAddress(address: Address, paging: Paging): S[F, TransactionInfo]

  /** Get total number of transactions related to a given `address`.
    */
  def countTxsInfoByAddress(address: Address): F[Int]

  /** Get all transactions appeared in the blockchain after the given `height`.
    */
  def getTxsSince(height: Int, paging: Paging): S[F, TransactionInfo]

  /** Get all ids matching the given `query`.
    */
  def getIdsLike(query: String): F[List[TxId]]

  /** Submit a transaction to the network.
    */
  def submitTransaction(tx: ErgoLikeTransaction): F[TxIdResponse]
}

object TransactionsService {

  def apply[F[_]: Concurrent, D[_]: Monad: LiftConnectionIO](
    utxCacheSettings: UtxCacheSettings,
    redis: RedisCommands[F, String, String]
  )(trans: D Trans F)(implicit e: ErgoAddressEncoder): F[TransactionsService[F, Stream]] =
    Slf4jLogger.create[F].flatMap { implicit logger =>
      ErgoLikeTransactionRepo[F](utxCacheSettings, redis).flatMap { etxRepo =>
        (
          HeaderRepo[F, D],
          TransactionRepo[F, D],
          UTransactionRepo[F, D],
          InputRepo[F, D],
          UInputRepo[F, D],
          OutputRepo[F, D],
          UOutputRepo[F, D],
          AssetRepo[F, D],
          UAssetRepo[F, D]
        ).mapN(new Live(_, _, _, _, _, _, _, _, _, etxRepo)(trans))
      }
    }

  final private class Live[F[_]: Logger: FlatMap, D[_]: Monad](
    headerRepo: HeaderRepo[D],
    transactionRepo: TransactionRepo[D, Stream],
    uTransactionRepo: UTransactionRepo[D, Stream],
    inputRepo: InputRepo[D],
    uInputRepo: UInputRepo[D, Stream],
    outputRepo: OutputRepo[D, Stream],
    uOutputRepo: UOutputRepo[D, Stream],
    assetRepo: AssetRepo[D, Stream],
    uAssetRepo: UAssetRepo[D],
    ergoLikeTxRepo: ErgoLikeTransactionRepo[F, Stream]
  )(trans: D Trans F)(implicit e: ErgoAddressEncoder)
    extends TransactionsService[F, Stream] {

    def getTxInfo(id: TxId): F[Option[TransactionSummary]] =
      (for {
        txOpt <- transactionRepo.getMain(id)
        ins   <- txOpt.toList.flatTraverse(tx => inputRepo.getAllByTxId(tx.id))
        outs  <- txOpt.toList.flatTraverse(tx => outputRepo.getAllByTxId(tx.id))
        boxIdsNel = outs.map(_.output.boxId).toNel
        assets     <- boxIdsNel.toList.flatTraverse(assetRepo.getAllByBoxIds)
        bestHeight <- headerRepo.getBestHeight
        txInfo = txOpt.map(tx =>
          TransactionSummary(tx, bestHeight - tx.inclusionHeight, ins, outs, assets)
        )
      } yield txInfo) ||> trans.xa

    def getUnconfirmedTxInfo(id: TxId): F[Option[UTransactionInfo]] =
      (for {
        txOpt <- uTransactionRepo.get(id)
        ins   <- txOpt.toList.flatTraverse(tx => uInputRepo.getAllByTxId(tx.id))
        outs  <- txOpt.toList.flatTraverse(tx => uOutputRepo.getAllByTxId(tx.id))
        boxIdsNel = outs.map(_.boxId).toNel
        assets <- boxIdsNel.toList.flatTraverse(uAssetRepo.getAllByBoxIds)
        txInfo = txOpt.map(UTransactionInfo(_, ins, outs, assets))
      } yield txInfo) ||> trans.xa

    def getTxsInfoByAddress(
      address: Address,
      paging: Paging
    ): Stream[F, TransactionInfo] =
      transactionRepo
        .getRelatedToAddress(address, paging.offset, paging.limit)
        .chunkN(100)
        .through(assembleInfo) ||> trans.xas

    def countTxsInfoByAddress(address: Address): F[Int] =
      transactionRepo.countRelatedToAddress(address) ||> trans.xa

    def getTxsSince(height: Int, paging: Paging): Stream[F, TransactionInfo] =
      transactionRepo
        .getMainSince(height, paging.offset, paging.limit)
        .chunkN(100)
        .through(assembleInfo) ||> trans.xas

    def getIdsLike(query: String): F[List[TxId]] =
      transactionRepo.getIdsLike(query) ||> trans.xa

    def submitTransaction(tx: ErgoLikeTransaction): F[TxIdResponse] =
      Logger[F].trace(s"Persisting ErgoLikeTransaction with id '${tx.id}'") >>
      ergoLikeTxRepo.put(tx) as TxIdResponse(tx.id.toString.coerce[TxId])

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
