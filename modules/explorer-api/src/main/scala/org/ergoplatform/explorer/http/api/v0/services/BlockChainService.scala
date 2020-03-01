package org.ergoplatform.explorer.http.api.v0.services

import cats.effect.Sync
import cats.instances.option._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.list._
import cats.syntax.traverse._
import cats.Monad
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import mouse.anyf._
import org.ergoplatform.explorer.Err.RequestProcessingErr.InconsistentDbData
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.Transaction
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.{Paging, Sorting}
import org.ergoplatform.explorer.http.api.v0.models.{BlockInfo, BlockReferencesInfo, BlockSummary, FullBlockInfo}
import org.ergoplatform.explorer.syntax.stream._
import org.ergoplatform.explorer.{CRaise, Id}
import tofu.syntax.raise._

/** A service providing an access to the blockchain data.
  */
trait BlockChainService[F[_], S[_[_], _]] {

  /** Get height of the best block.
    */
  def getBestHeight: F[Int]

  /** Get summary for a block with a given `id`.
    */
  def getBlockSummaryById(id: Id): F[Option[BlockSummary]]

  /** Get a slice block info items.
    */
  def getBlocks(paging: Paging, sorting: Sorting): S[F, BlockInfo]
}

object BlockChainService {

  def apply[
    F[_]: Sync,
    D[_]: LiftConnectionIO: CRaise[*[_], InconsistentDbData]: Monad
  ](xa: D Trans F): F[BlockChainService[F, Stream]] =
    Slf4jLogger
      .create[F]
      .map { implicit logger =>
        new Live(
          HeaderRepo[D],
          BlockInfoRepo[D],
          TransactionRepo[D],
          BlockExtensionRepo[D],
          AdProofRepo[D],
          TransactionRepo[D],
          InputRepo[D],
          OutputRepo[D],
          AssetRepo[D]
        )(xa)
      }

  final private class Live[
    F[_]: Sync: Logger,
    D[_]: CRaise[*[_], InconsistentDbData]: Monad
  ](
    headerRepo: HeaderRepo[D],
    blockInfoRepo: BlockInfoRepo[D, Stream],
    transactionRepo: TransactionRepo[D, Stream],
    blockExtensionRepo: BlockExtensionRepo[D],
    adProofRepo: AdProofRepo[D],
    txRepo: TransactionRepo[D, Stream],
    inputRepo: InputRepo[D],
    outputRepo: OutputRepo[D, Stream],
    assetRepo: AssetRepo[D, Stream]
  )(trans: D Trans F)
    extends BlockChainService[F, Stream] {

    def getBestHeight: F[Int] =
      (headerRepo.getBestHeight ||> trans.xa)
        .flatTap(h => Logger[F].trace(s"Reading best height from db: $h"))

    def getBlockSummaryById(id: Id): F[Option[BlockSummary]] = {
      val summary =
        for {
          blockInfoOpt <- getFullBlockInfo(id)
          parentOpt <- blockInfoOpt
                        .flatTraverse(h => headerRepo.getByParentId(h.headerInfo.id))
                        .asStream
        } yield
          blockInfoOpt.map { blockInfo =>
            val refs =
              BlockReferencesInfo(blockInfo.headerInfo.parentId, parentOpt.map(_.id))
            BlockSummary(blockInfo, refs)
          }

      (summary ||> trans.xas).compile.last.map(_.flatten)
    }

    def getBlocks(paging: Paging, sorting: Sorting): Stream[F, BlockInfo] =
      blockInfoRepo
        .getMany(paging.offset, paging.limit, sorting.order.value, sorting.sortBy)
        .map(BlockInfo(_)) ||> trans.xas

    private def getFullBlockInfo(id: Id): Stream[D, Option[FullBlockInfo]] =
      for {
        headerOpt <- headerRepo.get(id).asStream
        txs <- transactionRepo.getAllByBlockId(id).fold(List.empty[Transaction]) {
                 case (acc, tx) => tx +: acc
               }
        blockSizeOpt <- blockInfoRepo.getBlockSize(id).asStream
        bestHeight   <- headerRepo.getBestHeight.asStream
        txIdsNel     <- txs.map(_.id).toNel.orRaise[D](InconsistentDbData("Empty txs")).asStream
        inputs       <- inputRepo.getAllByTxIds(txIdsNel).asStream
        outputs      <- outputRepo.getAllByTxIds(txIdsNel).asStream
        boxIdsNel    <- outputs.map(_.output.boxId).toNel.orRaise[D](InconsistentDbData("Empty outputs")).asStream
        assets       <- assetRepo.getAllByBoxIds(boxIdsNel).asStream
        adProofsOpt  <- adProofRepo.getByHeaderId(id).asStream
        extensionOpt <- blockExtensionRepo.getByHeaderId(id).asStream
      } yield
        (headerOpt, blockSizeOpt, extensionOpt)
          .mapN { (header, size, ext) =>
            val numConfirmations = bestHeight - header.height
            FullBlockInfo(
              header,
              txs,
              numConfirmations,
              inputs,
              outputs,
              assets,
              ext,
              adProofsOpt,
              size
            )
          }
  }
}
