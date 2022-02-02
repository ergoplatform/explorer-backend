package org.ergoplatform.explorer.http.api.v0.services

import cats.Monad
import cats.data.{Chain, OptionT}
import cats.effect.Concurrent
import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.list._
import cats.syntax.traverse._
import dev.profunktor.redis4cats.algebra.RedisCommands
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.estatico.newtype.ops._
import mouse.any._
import mouse.anyf._
import org.ergoplatform.explorer.Err.RequestProcessingErr.BadRequest
import org.ergoplatform.explorer.Err.{RefinementFailed, RequestProcessingErr}
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.cache.repositories.ErgoLikeTransactionRepo
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.UTransaction
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.{Items, Paging, Sorting}
import org.ergoplatform.explorer.http.api.v0.models.{TxIdResponse, UTransactionInfo, UTransactionSummary}
import org.ergoplatform.explorer.protocol.TxValidation.PartialSemanticValidation
import org.ergoplatform.explorer.protocol.{TxValidation, sigma}
import org.ergoplatform.explorer.settings.UtxCacheSettings
import org.ergoplatform.{ErgoAddressEncoder, ErgoLikeTransaction}
import tofu.syntax.raise._

/** A service providing an access to unconfirmed transactions data.
  */
trait OffChainService[F[_]] {

  /** Get unconfirmed transactions.
    */
  def getUnconfirmedTxs(paging: Paging, sorting: Sorting): F[Items[UTransactionInfo]]

  /** Get unconfirmed transaction with a given `id`.
    */
  def getUnconfirmedTxInfo(id: TxId): F[Option[UTransactionSummary]]

  /** Get all unconfirmed transactions related to the given `address`.
    */
  def getUnconfirmedTxsByAddress(address: Address, paging: Paging): F[Items[UTransactionInfo]]

  /** Get all unconfirmed transactions related to the given `ergoTree`.
    */
  def getUnconfirmedTxsByErgoTree(
    ergoTree: HexString,
    paging: Paging
  ): F[Items[UTransactionInfo]]

  /** Submit a transaction to the network.
    */
  def submitTransaction(tx: ErgoLikeTransaction): F[TxIdResponse]
}

object OffChainService {

  def apply[F[_]: Concurrent, D[_]: Monad: LiftConnectionIO](
    utxCacheSettings: UtxCacheSettings,
    redis: Option[RedisCommands[F, String, String]]
  )(
    trans: D Trans F
  )(implicit e: ErgoAddressEncoder): F[OffChainService[F]] =
    Slf4jLogger.create[F].flatMap { implicit logger =>
      val validation = PartialSemanticValidation
      redis.map(ErgoLikeTransactionRepo[F](utxCacheSettings, _)).sequence.flatMap { etxRepo =>
        (
          TransactionRepo[F, D],
          UTransactionRepo[F, D],
          UInputRepo[F, D],
          UDataInputRepo[F, D],
          UOutputRepo[F, D],
          UAssetRepo[F, D]
        ).mapN(new Live(_, _, _, _, _, _, etxRepo, validation)(trans))
      }
    }

  final private class Live[
    F[_]: CRaise[*[_], RequestProcessingErr]: CRaise[*[_], RefinementFailed]: Monad: Logger,
    D[_]: Monad
  ](
    txRepo: TransactionRepo[D, Stream],
    uTxRepo: UTransactionRepo[D, Stream],
    inRepo: UInputRepo[D, Stream],
    dataInRepo: UDataInputRepo[D, Stream],
    outRepo: UOutputRepo[D, Stream],
    assetRepo: UAssetRepo[D],
    ergoLikeTxRepo: Option[ErgoLikeTransactionRepo[F, Stream]],
    validation: TxValidation
  )(trans: D Trans F)(implicit e: ErgoAddressEncoder)
    extends OffChainService[F] {

    def getUnconfirmedTxs(paging: Paging, sorting: Sorting): F[Items[UTransactionInfo]] =
      uTxRepo.countAll.flatMap { total =>
        txRepo.getRecentIds.flatMap { recentlyConfirmed =>
          uTxRepo
            .getAll(paging.offset, paging.limit, sorting.order.value, sorting.sortBy)
            .map(_.grouped(100))
            .flatMap(_.toList.flatTraverse(assembleUInfo))
            .map(confirmedDiff(_, total)(recentlyConfirmed))
        }
      } ||> trans.xa

    def getUnconfirmedTxInfo(id: TxId): F[Option[UTransactionSummary]] =
      (for {
        txOpt <- uTxRepo.get(id)
        txs = txOpt.toList
        ins     <- txs.flatTraverse(tx => inRepo.getAllByTxId(tx.id))
        dataIns <- txs.flatTraverse(tx => dataInRepo.getAllByTxId(tx.id))
        outs    <- txs.flatTraverse(tx => outRepo.getAllByTxId(tx.id))
        boxIdsNel = outs.map(_.output.boxId).toNel
        assets <- boxIdsNel.toList.flatTraverse(assetRepo.getAllByBoxIds)
        txInfo = txOpt.map(UTransactionSummary(_, ins, dataIns, outs, assets))
      } yield txInfo) ||> trans.xa

    def getUnconfirmedTxsByAddress(
      address: Address,
      paging: Paging
    ): F[Items[UTransactionInfo]] =
      sigma.addressToErgoTreeHex(address) |> (getUnconfirmedTxsByErgoTree(_, paging))

    def getUnconfirmedTxsByErgoTree(
      ergoTree: HexString,
      paging: Paging
    ): F[Items[UTransactionInfo]] =
      uTxRepo.countByErgoTree(ergoTree).flatMap { total =>
        txRepo.getRecentIds.flatMap { recentlyConfirmed =>
          uTxRepo
            .getAllRelatedToErgoTree(ergoTree, paging.offset, paging.limit)
            .map(_.grouped(100))
            .flatMap(_.toList.flatTraverse(assembleUInfo))
            .map(confirmedDiff(_, total)(recentlyConfirmed))
        }
      } ||> trans.xa

    def submitTransaction(tx: ErgoLikeTransaction): F[TxIdResponse] =
      ergoLikeTxRepo match {
        case Some(repo) =>
          val errors = validation.validate(tx)
          if (errors.isEmpty)
            Logger[F].info(s"Persisting ErgoLikeTransaction with id '${tx.id}'") >>
            repo.put(tx) as TxIdResponse(tx.id.toString.coerce[TxId])
          else
            Logger[F].info(s"Rejecting ErgoLikeTransaction with id '${tx.id}'") >>
            BadRequest(s"Transaction is invalid. ${errors.mkString("; ")}").raise
        case None =>
          BadRequest("Transaction broadcasting is disabled").raise
      }

    private def assembleUInfo: List[UTransaction] => D[List[UTransactionInfo]] =
      txChunk =>
        (for {
          txIdsNel  <- OptionT.fromOption[D](txChunk.map(_.id).toNel)
          ins       <- OptionT.liftF(inRepo.getAllByTxIds(txIdsNel))
          dataIns   <- OptionT.liftF(dataInRepo.getAllByTxIds(txIdsNel))
          outs      <- OptionT.liftF(outRepo.getAllByTxIds(txIdsNel))
          boxIdsNel <- OptionT.fromOption[D](outs.map(_.output.boxId).toNel)
          assets    <- OptionT.liftF(assetRepo.getAllByBoxIds(boxIdsNel))
          txInfo = UTransactionInfo.batch(txChunk, ins, dataIns, outs, assets)
        } yield txInfo).value.map(_.toList.flatten)

    private def confirmedDiff(
      txs: List[UTransactionInfo],
      total: Int
    )(confirmedIds: List[TxId]): Items[UTransactionInfo] = {
      val filter = confirmedIds.toSet
      val (unconfirmed, filteredQty) =
        txs.foldLeft(Chain.empty[UTransactionInfo] -> 0) { case ((acc, c), tx) =>
          if (filter.contains(tx.id)) acc -> (c + 1)
          else (acc :+ tx)                -> c
        }
      Items(unconfirmed.toList, total - filteredQty)
    }
  }
}
