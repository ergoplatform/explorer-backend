package org.ergoplatform.explorer.http.api.v0.services

import cats.data.OptionT
import cats.effect.Concurrent
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.list._
import cats.syntax.apply._
import cats.syntax.traverse._
import cats.{FlatMap, Monad}
import dev.profunktor.redis4cats.algebra.RedisCommands
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.estatico.newtype.ops._
import mouse.anyf._
import org.ergoplatform.explorer.cache.repositories.ErgoLikeTransactionRepo
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.{Transaction, UTransaction}
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.v0.models.{
  TransactionInfo,
  TransactionSummary,
  TxIdResponse,
  UTransactionInfo
}
import org.ergoplatform.explorer.settings.UtxCacheSettings
import org.ergoplatform.explorer.{Address, TxId}
import org.ergoplatform.{ErgoAddressEncoder, ErgoLikeTransaction}

/** A service providing an access to the transactions data.
  */
trait TransactionsService[F[_]] {

  /** Get transaction info by `id`.
    */
  def getTxInfo(id: TxId): F[Option[TransactionSummary]]

  /** Get unconfirmed transactions.
    */
  def getUnconfirmedTxs(paging: Paging): F[Items[UTransactionInfo]]

  /** Get unconfirmed transaction info by `id`.
    */
  def getUnconfirmedTxInfo(id: TxId): F[Option[UTransactionInfo]]

  /** Get transactions related to a given `address`.
    */
  def getTxsInfoByAddress(address: Address, paging: Paging): F[Items[TransactionInfo]]

  /** Get all transactions appeared in the blockchain after the given `height`.
    */
  def getTxsSince(height: Int, paging: Paging): F[List[TransactionInfo]]

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
  )(trans: D Trans F)(implicit e: ErgoAddressEncoder): F[TransactionsService[F]] =
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
    extends TransactionsService[F] {

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

    def getUnconfirmedTxs(paging: Paging): F[Items[UTransactionInfo]] =
      uTransactionRepo.countAll.flatMap { total =>
        uTransactionRepo
          .getAll(paging.offset, paging.limit)
          .map(_.grouped(100))
          .flatMap(_.toList.flatTraverse(assembleUInfo))
          .map(Items(_, total))
      } ||> trans.xa

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
    ): F[Items[TransactionInfo]] =
      transactionRepo.countRelatedToAddress(address).flatMap { total =>
        transactionRepo
          .getRelatedToAddress(address, paging.offset, paging.limit)
          .map(_.grouped(100))
          .flatMap(_.toList.flatTraverse(assembleInfo))
          .map(Items(_, total))
      } ||> trans.xa

    def getTxsSince(height: Int, paging: Paging): F[List[TransactionInfo]] =
      transactionRepo
        .getMainSince(height, paging.offset, paging.limit)
        .map(_.grouped(100))
        .flatMap(_.toList.flatTraverse(assembleInfo)) ||> trans.xa

    def getIdsLike(query: String): F[List[TxId]] =
      transactionRepo.getIdsLike(query) ||> trans.xa

    def submitTransaction(tx: ErgoLikeTransaction): F[TxIdResponse] =
      Logger[F].trace(s"Persisting ErgoLikeTransaction with id '${tx.id}'") >>
      ergoLikeTxRepo.put(tx) as TxIdResponse(tx.id.toString.coerce[TxId])

    private def assembleInfo: List[Transaction] => D[List[TransactionInfo]] =
      txChunk =>
        (for {
          txIdsNel   <- OptionT.fromOption[D](txChunk.map(_.id).toNel)
          ins        <- OptionT.liftF(inputRepo.getAllByTxIds(txIdsNel))
          outs       <- OptionT.liftF(outputRepo.getAllByTxIds(txIdsNel))
          boxIdsNel  <- OptionT.fromOption[D](outs.map(_.output.boxId).toNel)
          assets     <- OptionT.liftF(assetRepo.getAllByBoxIds(boxIdsNel))
          bestHeight <- OptionT.liftF(headerRepo.getBestHeight)
          txsWithHeights = txChunk.map(tx => tx -> (bestHeight - tx.inclusionHeight))
          txInfo         = TransactionInfo.batch(txsWithHeights, ins, outs, assets)
        } yield txInfo).value.map(_.toList.flatten)

    private def assembleUInfo: List[UTransaction] => D[List[UTransactionInfo]] =
      txChunk =>
        (for {
          txIdsNel  <- OptionT.fromOption[D](txChunk.map(_.id).toNel)
          ins       <- OptionT.liftF(uInputRepo.getAllByTxIds(txIdsNel))
          outs      <- OptionT.liftF(uOutputRepo.getAllByTxIds(txIdsNel))
          boxIdsNel <- OptionT.fromOption[D](outs.map(_.boxId).toNel)
          assets    <- OptionT.liftF(uAssetRepo.getAllByBoxIds(boxIdsNel))
          txInfo = UTransactionInfo.batch(txChunk, ins, outs, assets)
        } yield txInfo).value.map(_.toList.flatten)
  }
}
