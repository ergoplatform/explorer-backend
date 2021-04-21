package org.ergoplatform.explorer.http.api.v1.services

import cats.effect.Sync
import cats.instances.option._
import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.list._
import cats.syntax.traverse._
import cats.{FlatMap, Monad}
import fs2.Stream
import mouse.anyf._
import org.ergoplatform.explorer.Err.RequestProcessingErr.InconsistentDbData
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.Transaction
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.{Items, Paging, Sorting}
import org.ergoplatform.explorer.http.api.v0.models.{BlockInfo, BlockReferencesInfo, BlockSummary, FullBlockInfo}
import org.ergoplatform.explorer.syntax.stream._
import org.ergoplatform.explorer.{CRaise, Id}
import tofu.syntax.raise._

trait Blocks[F[_]] {

  /** Get a slice of block info items.
    */
  def getBlocks(paging: Paging, sorting: Sorting): F[Items[BlockInfo]]

  /** Get summary for a block with a given `id`.
    */
  def getBlockSummaryById(id: Id): F[Option[BlockSummary]]
}

object Blocks {

  def apply[
    F[_]: Sync,
    D[_]: LiftConnectionIO: CRaise[*[_], InconsistentDbData]: Monad
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
    F[_]: Sync,
    D[_]: CRaise[*[_], InconsistentDbData]: Monad
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
    override def getBlocks(paging: Paging, sorting: Sorting): F[Items[BlockInfo]] =
      headerRepo.getBestHeight.flatMap { total =>
        blockInfoRepo
          .getMany(paging.offset, paging.limit, sorting.order.value, sorting.sortBy)
          .map(_.map(BlockInfo(_)))
          .map(Items(_, total))
      } ||> trans.xa

    def getBlockSummaryById(id: Id): F[Option[BlockSummary]] = {
      val summary =
        for {
          blockInfoOpt <- getFullBlockInfo(id)
          parentOpt <- blockInfoOpt
            .flatTraverse(h => headerRepo.getByParentId(h.header.id))
            .asStream
        } yield blockInfoOpt.map { blockInfo =>
          val refs =
            BlockReferencesInfo(blockInfo.header.parentId, parentOpt.map(_.id))
          BlockSummary(blockInfo, refs)
        }

      (summary ||> trans.xas).compile.last.map(_.flatten)
    }

    private def getFullBlockInfo(id: Id): Stream[D, Option[FullBlockInfo]] =
      for {
        header       <- headerRepo.get(id).asStream.unNone
        txs          <- transactionRepo.getAllByBlockId(id).fold(Array.empty[Transaction])(_ :+ _).map(_.toList)
        blockSizeOpt <- blockInfoRepo.getBlockSize(id).asStream
        bestHeight   <- headerRepo.getBestHeight.asStream
        txIdsNel     <- txs.map(_.id).toNel.orRaise[D](InconsistentDbData("Empty txs")).asStream
        inputs       <- inputRepo.getAllByTxIds(txIdsNel).asStream
        dataInputs   <- dataInputRepo.getAllByTxIds(txIdsNel).asStream
        outputs      <- outputRepo.getAllByTxIds(txIdsNel).asStream
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
