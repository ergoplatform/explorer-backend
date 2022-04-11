package org.ergoplatform.explorer.http.api.v1.services

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.traverse._
import cats.syntax.list._
import cats.{FlatMap, Monad}
import fs2.{Chunk, Pipe, Stream}
import mouse.anyf._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.Transaction
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.Sorting.SortOrder
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v1.models.TransactionInfo
import org.ergoplatform.explorer.settings.ServiceSettings
import org.ergoplatform.explorer.{Address, ErgoTreeTemplateHash, TxId}
import tofu.syntax.monadic._
import tofu.syntax.streams.compile._

trait Transactions[F[_]] {

  def get(id: TxId): F[Option[TransactionInfo]]

  def getByInputsScriptTemplate(
    template: ErgoTreeTemplateHash,
    paging: Paging,
    ordering: SortOrder
  ): F[Items[TransactionInfo]]

  def getByAddress(
    address: Address,
    paging: Paging,
    concise: Boolean
  ): F[Items[TransactionInfo]]

  def streamAll(minGix: Long, limit: Int): Stream[F, TransactionInfo]
}

object Transactions {

  val MaxIdsPerRequest = scala.Short.MaxValue / 4

  def apply[
    F[_]: Sync,
    D[_]: LiftConnectionIO: Monad: CompileStream
  ](serviceSettings: ServiceSettings)(trans: D Trans F)(implicit e: ErgoAddressEncoder): F[Transactions[F]] =
    (AssetRepo[F, D], InputRepo[F, D], DataInputRepo[F, D], OutputRepo[F, D], TransactionRepo[F, D], HeaderRepo[F, D])
      .mapN(
        new Live(serviceSettings, _, _, _, _, _, _)(trans)
      )

  final class Live[F[_]: FlatMap, D[_]: Monad: CompileStream](
    serviceSettings: ServiceSettings,
    assets: AssetRepo[D, Stream],
    inputs: InputRepo[D],
    dataInputs: DataInputRepo[D],
    outputs: OutputRepo[D, Stream],
    transactions: TransactionRepo[D, Stream],
    headers: HeaderRepo[D]
  )(trans: D Trans F)
    extends Transactions[F] {

    def get(id: TxId): F[Option[TransactionInfo]] = {
      val getTx =
        for {
          tx         <- OptionT(transactions.getMain(id))
          ins        <- OptionT.liftF(inputs.getFullByTxId(id))
          dataIns    <- OptionT.liftF(dataInputs.getFullByTxId(id))
          inIds      <- OptionT.fromOption(ins.map(_.input.boxId).toNel)
          inAssets   <- OptionT.liftF(assets.getAllByBoxIds(inIds))
          outs       <- OptionT.liftF(outputs.getAllByTxId(id))
          outIds     <- OptionT.fromOption(outs.map(_.output.boxId).toNel)
          outAssets  <- OptionT.liftF(assets.getAllByBoxIds(outIds))
          bestHeight <- OptionT.liftF(headers.getBestHeight)
          numConfirmations = tx.numConfirmations(bestHeight)
        } yield TransactionInfo.unFlatten(tx, numConfirmations, ins, dataIns, outs, inAssets, outAssets)
      getTx.value.thrushK(trans.xa)
    }

    def getByInputsScriptTemplate(
      template: ErgoTreeTemplateHash,
      paging: Paging,
      ordering: SortOrder
    ): F[Items[TransactionInfo]] =
      transactions
        .countByInputsScriptTemplate(template)
        .flatMap { total =>
          transactions
            .getByInputsScriptTemplate(template, paging.offset, paging.limit, ordering.value)
            .chunkN(serviceSettings.chunkSize)
            .through(makeTransaction(None))
            .to[List]
            .map(Items(_, total))
        }
        .thrushK(trans.xa)

    def getByAddress(
      address: Address,
      paging: Paging,
      concise: Boolean
    ): F[Items[TransactionInfo]] =
      transactions
        .countRelatedToAddress(address)
        .flatMap { total =>
          val narrowBy = if (concise) Some(address) else None
          transactions
            .streamRelatedToAddress(address, paging.offset, paging.limit)
            .chunkN(serviceSettings.chunkSize)
            .through(makeTransaction(narrowBy))
            .to[List]
            .map(Items(_, total))
        }
        .thrushK(trans.xa)

    def streamAll(minGix: Long, limit: Int): Stream[F, TransactionInfo] =
      transactions
        .streamTransactions(minGix, limit)
        .chunkN(serviceSettings.chunkSize)
        .through(makeTransaction(None))
        .thrushK(trans.xas)

    private def makeTransaction(narrowByAddress: Option[Address]): Pipe[D, Chunk[Transaction], TransactionInfo] =
      for {
        chunk      <- _
        txIds      <- Stream.emit(chunk.map(_.id).toNel).unNone
        ins        <- Stream.eval(inputs.getFullByTxIds(txIds, narrowByAddress))
        inIds      <- Stream.emit(ins.map(_.input.boxId).toNel).unNone
        inAssets   <- Stream.eval(inIds.grouped(MaxIdsPerRequest).toList.flatTraverse(assets.getAllByBoxIds))
        dataIns    <- Stream.eval(dataInputs.getFullByTxIds(txIds))
        outs       <- Stream.eval(outputs.getAllByTxIds(txIds, narrowByAddress))
        outIds     <- Stream.emit(outs.map(_.output.boxId).toNel).unNone
        outAssets  <- Stream.eval(outIds.grouped(MaxIdsPerRequest).toList.flatTraverse(assets.getAllByBoxIds))
        bestHeight <- Stream.eval(headers.getBestHeight)
        txsWithHeights = chunk.map(tx => tx -> tx.numConfirmations(bestHeight))
        txInfo <-
          Stream.emits(TransactionInfo.unFlattenBatch(txsWithHeights.toList, ins, dataIns, outs, inAssets, outAssets))
      } yield txInfo
  }
}
