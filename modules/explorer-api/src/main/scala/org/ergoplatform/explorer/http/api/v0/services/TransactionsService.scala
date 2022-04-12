package org.ergoplatform.explorer.http.api.v0.services

import cats.data.OptionT
import cats.effect.Sync
import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.list._
import cats.syntax.traverse._
import cats.{FlatMap, Monad}
import dev.profunktor.redis4cats.algebra.RedisCommands
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import mouse.anyf._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.Transaction
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.v0.models.{TransactionInfo, TransactionSummary}
import org.ergoplatform.explorer.settings.UtxCacheSettings
import org.ergoplatform.explorer.{Address, TxId}

/** A service providing an access to the transactions data.
  */
trait TransactionsService[F[_]] {

  /** Get transaction info by `id`.
    */
  def getTxInfo(id: TxId): F[Option[TransactionSummary]]

  /** Get transactions related to a given `address`.
    */
  def getTxsInfoByAddress(address: Address, paging: Paging, concise: Boolean): F[Items[TransactionInfo]]

  /** Get all transactions appeared in the blockchain after the given `height`.
    */
  def getTxsSince(height: Int, paging: Paging): F[List[TransactionInfo]]

  /** Get all ids matching the given `query`.
    */
  def getIdsLike(query: String): F[List[TxId]]
}

object TransactionsService {

  def apply[F[_]: Sync, D[_]: Monad: LiftConnectionIO](
    trans: D Trans F
  )(implicit e: ErgoAddressEncoder): F[TransactionsService[F]] =
    Slf4jLogger.create[F].flatMap { implicit logger =>
      (
        HeaderRepo[F, D],
        TransactionRepo[F, D],
        InputRepo[F, D],
        DataInputRepo[F, D],
        OutputRepo[F, D],
        AssetRepo[F, D]
      ).mapN(new Live(_, _, _, _, _, _)(trans))
    }

  final private class Live[F[_]: Logger: FlatMap, D[_]: Monad](
    headerRepo: HeaderRepo[D],
    transactionRepo: TransactionRepo[D, Stream],
    inputRepo: InputRepo[D],
    dataInputRepo: DataInputRepo[D],
    outputRepo: OutputRepo[D, Stream],
    assetRepo: AssetRepo[D, Stream]
  )(trans: D Trans F)(implicit e: ErgoAddressEncoder)
    extends TransactionsService[F] {

    def getTxInfo(id: TxId): F[Option[TransactionSummary]] =
      (for {
        txOpt   <- transactionRepo.getMain(id)
        ins     <- txOpt.toList.flatTraverse(tx => inputRepo.getAllByTxId(tx.id))
        dataIns <- txOpt.toList.flatTraverse(tx => dataInputRepo.getAllByTxId(tx.id))
        outs    <- txOpt.toList.flatTraverse(tx => outputRepo.getAllByTxId(tx.id))
        boxIdsNel = outs.map(_.output.boxId).toNel
        assets     <- boxIdsNel.toList.flatTraverse(assetRepo.getAllByBoxIds)
        bestHeight <- headerRepo.getBestHeight
        txInfo = txOpt.map(tx => TransactionSummary(tx, tx.numConfirmations(bestHeight), ins, dataIns, outs, assets))
      } yield txInfo) ||> trans.xa

    def getTxsInfoByAddress(
      address: Address,
      paging: Paging,
      concise: Boolean
    ): F[Items[TransactionInfo]] =
      transactionRepo.countRelatedToAddress(address).flatMap { total =>
        transactionRepo
          .getRelatedToAddress(address, paging.offset, paging.limit)
          .map(_.grouped(100))
          .flatMap(_.toList.flatTraverse(assembleInfo(concise)))
          .map(Items(_, total))
      } ||> trans.xa

    def getTxsSince(height: Int, paging: Paging): F[List[TransactionInfo]] =
      transactionRepo
        .getMainSince(height, paging.offset, paging.limit)
        .map(_.grouped(100))
        .flatMap(_.toList.flatTraverse(assembleInfo())) ||> trans.xa

    def getIdsLike(query: String): F[List[TxId]] =
      transactionRepo.getIdsLike(query) ||> trans.xa

    private def assembleInfo(concise: Boolean = false): List[Transaction] => D[List[TransactionInfo]] =
      txChunk =>
        (for {
          txIdsNel <- OptionT.fromOption[D](txChunk.map(_.id).toNel)
          (ins, dataIns, outs, assets) <-
            if (!concise) for {
              ins       <- OptionT.liftF(inputRepo.getAllByTxIds(txIdsNel))
              dataIns   <- OptionT.liftF(dataInputRepo.getAllByTxIds(txIdsNel))
              outs      <- OptionT.liftF(outputRepo.getAllByTxIds(txIdsNel, None))
              boxIdsNel <- OptionT.fromOption[D](outs.map(_.output.boxId).toNel)
              assets    <- OptionT.liftF(assetRepo.getAllByBoxIds(boxIdsNel))
            } yield (ins, dataIns, outs, assets)
            else OptionT.pure[D]((List.empty, List.empty, List.empty, List.empty))
          bestHeight <- OptionT.liftF(headerRepo.getBestHeight)
          txsWithHeights = txChunk.map(tx => tx -> tx.numConfirmations(bestHeight))
          txInfo         = TransactionInfo.batch(txsWithHeights, ins, dataIns, outs, assets)
        } yield txInfo).value.map(_.toList.flatten)
  }
}
