package org.ergoplatform.explorer.http.api.v1.services

import cats.effect.Sync
import cats.instances.option._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.list._
import cats.syntax.traverse._
import cats.{Functor, Monad}
import fs2.Stream
import mouse.anyf._
import org.ergoplatform.explorer.Err.RequestProcessingErr.InconsistentDbData
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.Transaction
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.{Items, Paging, Sorting}
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v0.models.{BlockReferencesInfo, BlockSummary, FullBlockInfo}
import org.ergoplatform.explorer.http.api.v1.models.{BlockHeader, BlockInfo}
import org.ergoplatform.explorer.syntax.stream._
import org.ergoplatform.explorer.{BlockId, CRaise}
import tofu.syntax.raise._
import tofu.syntax.streams.compile._

trait Blocks[F[_]] {

  /** Get a slice of block info items.
    */
  def getBlocks(paging: Paging, sorting: Sorting): F[Items[BlockInfo]]

  /** Get summary for a block with a given `id`.
    */
  def getBlockSummaryById(id: BlockId): F[Option[BlockSummary]]

  /** Stream blocks
    */
  def streamBlocks(minGix: Long, limit: Int): Stream[F, BlockInfo]
  
  /** Get a slice of block header items.
    */
  def getBlockHeaders(paging: Paging, sorting: Sorting): F[Items[BlockHeader]]
}

object Blocks {

  def apply[
    F[_]: Sync,
    D[_]: Monad: LiftConnectionIO: CRaise[*[_], InconsistentDbData]: CompileStream
  ](trans: D Trans F): F[Blocks[F]] =
    (
      HeaderRepo[F, D],
      BlockInfoRepo[F, D],
      TransactionRepo[F, D],
      BlockExtensionRepo[F, D],
      AdProofRepo[F, D],
      InputRepo[F, D],
      DataInputRepo[F, D],
      OutputRepo[F, D],
      AssetRepo[F, D]
    ).mapN {
      new Live[F, D](_, _, _, _, _, _, _, _, _)(trans)
    }

  final private class Live[
    F[_]: Functor,
    D[_]: CRaise[*[_], InconsistentDbData]: Monad: CompileStream
  ](
    headerRepo: HeaderRepo[D],
    blockInfoRepo: BlockInfoRepo[D],
    transactionRepo: TransactionRepo[D, Stream],
    blockExtensionRepo: BlockExtensionRepo[D],
    adProofRepo: AdProofRepo[D],
    inputRepo: InputRepo[D],
    dataInputRepo: DataInputRepo[D],
    outputRepo: OutputRepo[D, Stream],
    assetRepo: AssetRepo[D, Stream]
  )(trans: D Trans F)
    extends Blocks[F] {

    /** Get a slice of block info items.
      */
    def getBlocks(paging: Paging, sorting: Sorting): F[Items[BlockInfo]] =
      headerRepo.getBestHeight.flatMap { total =>
        blockInfoRepo
          .getMany(paging.offset, paging.limit, sorting.order.value, sorting.sortBy)
          .map(_.map(BlockInfo(_)))
          .map(Items(_, total))
      } ||> trans.xa

    def getBlockSummaryById(id: BlockId): F[Option[BlockSummary]] = {
      val summary =
        for {
          blockInfoOpt <- getFullBlockInfo(id)
          ancestorOpt <- blockInfoOpt
                           .flatTraverse(h => headerRepo.getByParentId(h.header.id))
                           .asStream
        } yield blockInfoOpt.map { blockInfo =>
          val refs =
            BlockReferencesInfo(blockInfo.header.parentId, ancestorOpt.map(_.id))
          BlockSummary(blockInfo, refs)
        }

      summary.to[List].map(_.headOption.flatten).thrushK(trans.xa)
    }

    def streamBlocks(minGix: Long, limit: Int): Stream[F, BlockInfo] =
      blockInfoRepo.stream(minGix, limit).map(BlockInfo(_)).thrushK(trans.xas)

    def getBlockHeaders(paging: Paging, sorting: Sorting): F[Items[BlockHeader]] = {
      headerRepo.getBestHeight.flatMap { total =>
        headerRepo.getMany(paging.offset, paging.limit, sorting.order.value, sorting.sortBy)
          .map(_.map(BlockHeader(_)))
          .map(Items(_, total))
      } ||> trans.xa
    }

    private def getFullBlockInfo(id: BlockId): Stream[D, Option[FullBlockInfo]] =
      for {
        header       <- headerRepo.get(id).asStream.unNone
        txs          <- transactionRepo.getAllByBlockId(id).fold[List[Transaction]](List.empty[Transaction])(_ :+ _)
        blockSizeOpt <- blockInfoRepo.getBlockSize(id).asStream
        bestHeight   <- headerRepo.getBestHeight.asStream
        txIdsNel     <- txs.map(_.id).toNel.orRaise[D](InconsistentDbData("Empty txs")).asStream
        inputs       <- inputRepo.getAllByTxIds(txIdsNel).asStream
        dataInputs   <- dataInputRepo.getAllByTxIds(txIdsNel).asStream
        outputs      <- outputRepo.getAllByTxIds(txIdsNel, None).asStream
        boxIdsNel    <- outputs.map(_.output.boxId).toNel.orRaise[D](InconsistentDbData("Empty outputs")).asStream
        assets       <- assetRepo.getAllByBoxIds(boxIdsNel).asStream
        adProofsOpt  <- adProofRepo.getByHeaderId(id).asStream
        extensionOpt <- blockExtensionRepo.getByHeaderId(id).asStream
      } yield (blockSizeOpt, extensionOpt)
        .mapN { (size, ext) =>
          val numConfirmations = bestHeight - header.height + 1
          FullBlockInfo(
            header,
            txs,
            numConfirmations,
            inputs,
            dataInputs,
            outputs,
            assets,
            ext,
            adProofsOpt,
            size
          )
        }
  }
}
