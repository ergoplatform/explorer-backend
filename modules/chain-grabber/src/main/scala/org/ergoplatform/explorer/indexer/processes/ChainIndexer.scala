package org.ergoplatform.explorer.indexer.processes

import cats.data.OptionT
import cats.effect.concurrent.Ref
import cats.effect.syntax.bracket._
import cats.effect.{Bracket, Concurrent, Sync, Timer}
import cats.syntax.option._
import cats.instances.list._
import cats.syntax.foldable._
import cats.syntax.parallel._
import cats.syntax.traverse._
import cats.{Monad, Parallel}
import fs2.Stream
import fs2.concurrent.Queue
import mouse.anyf._
import org.ergoplatform.explorer.BlockId
import org.ergoplatform.explorer.BuildFrom.syntax._
import org.ergoplatform.explorer.Err.ProcessingErr.{InconsistentNodeView, NoBlocksWritten}
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.{BlockStats, Header}
import org.ergoplatform.explorer.indexer.extractors._
import org.ergoplatform.explorer.indexer.models.{FlatBlock, SlotData, TotalStats}
import org.ergoplatform.explorer.indexer.modules.RepoBundle
import org.ergoplatform.explorer.protocol.constants.GenesisHeight
import org.ergoplatform.explorer.protocol.models.ApiFullBlock
import org.ergoplatform.explorer.services.ErgoNetwork
import org.ergoplatform.explorer.settings.{IndexerSettings, ProtocolSettings}
import tofu.concurrent.MakeRef
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.raise._
import tofu.{Context, MonadThrow, WithContext}

import scala.concurrent.duration.DurationInt

/** Synchronizes local view with the Ergo network.
  */
trait ChainIndexer[F[_]] {

  def run: Stream[F, Unit]
}

object ChainIndexer {

  val SyncQSize      = 16
  val PrefetchBlocks = 12

  def apply[F[_]: Concurrent: Parallel: Timer, D[_]: MonadThrow: LiftConnectionIO](
    settings: IndexerSettings,
    network: ErgoNetwork[F]
  )(trans: Trans[D, F])(implicit logs: Logs[F, F], makeRef: MakeRef[F, F]): F[ChainIndexer[F]] =
    logs.forService[ChainIndexer[F]].flatMap { implicit log =>
      for {
        updatesRef     <- makeRef.refOf(List.empty[(BlockId, Int)])
        lastBlockCache <- makeRef.refOf(List.empty[(Header, BlockStats)])
        bestHeightRef  <- makeRef.refOf(none[Int])
        blocksBufferR  <- makeRef.refOf(Map.empty[Int, (List[ApiFullBlock], List[ApiFullBlock])])
        syncQueue      <- Queue.bounded[F, (ApiFullBlock, List[ApiFullBlock])](SyncQSize)
        repos          <- RepoBundle[F, D]
      } yield new Live[F, D](
        settings,
        network,
        updatesRef,
        lastBlockCache,
        bestHeightRef,
        blocksBufferR,
        syncQueue,
        repos
      )(trans)
    }

  final class Live[
    F[_]: Concurrent: Parallel: Timer: Bracket[*[_], Throwable]: Logging,
    D[_]: Monad
  ](
    settings: IndexerSettings,
    network: ErgoNetwork[F],
    pendingChainUpdates: Ref[F, List[(BlockId, Int)]],
    lastBlockCache: Ref[F, List[(Header, BlockStats)]],
    bestHeightR: Ref[F, Option[Int]],
    blocksBufferR: Ref[F, Map[Int, (List[ApiFullBlock], List[ApiFullBlock])]],
    syncQueue: Queue[F, (ApiFullBlock, List[ApiFullBlock])],
    repos: RepoBundle[D]
  )(trans: Trans[D, F])
    extends ChainIndexer[F] {

    private val startHeight = settings.startHeight getOrElse GenesisHeight

    def run: Stream[F, Unit] =
      Stream(()).repeat
        .covary[F]
        .metered(settings.pollInterval)
        .flatMap { _ =>
          Stream.eval(info"Starting sync job ..") >> sync.handleErrorWith { e =>
            Stream.eval(
              warnCause"An error occurred while syncing with the network. Restarting ..." (e)
            )
          }
        }

    def sync: Stream[F, Unit] =
      for {
        networkHeight <- Stream.eval(network.getBestHeight)
        localHeight   <- Stream.eval(getLastGrabbedBlockHeight)
        lowerHeight = (localHeight + 1) max startHeight
        _ <- Stream.eval(info"Current network height : $networkHeight")
        _ <- Stream.eval(info"Current explorer height: $localHeight")
        prefetch = Stream
                     .range(lowerHeight, networkHeight + 1, PrefetchBlocks)
                     .flatMap(i => prefetchBlocks(i, i + PrefetchBlocks - 1))
        pull    = Stream.eval(pullBlocks(lowerHeight, networkHeight))
        process = syncQueue.dequeue.evalMap { case (best, orphans) => index(best, orphans) }
        _ <- Stream.emits(List(prefetch, pull, process)).parJoinUnbounded
      } yield ()

    private def prefetchBlocks(lower: Int, upper: Int): Stream[F, Unit] =
      Stream
        .range(lower, upper + 1)
        .covary[F]
        .map { height =>
          val prefetch = for {
            ids     <- network.getBlockIdsAtHeight(height)
            blocks0 <- ids.parTraverse(network.getFullBlockById)
            blocks     = blocks0.flatten
            blocksPart = blocks.partition(b => ids.headOption.contains(b.header.id))
            _ <- blocksBufferR.update(_.updated(height, blocksPart))
          } yield ()
          Stream.eval(prefetch)
        }
        .parJoinUnbounded

    private def pullBlocks(lower: Int, upper: Int): F[Unit] =
      for {
        _          <- info"Pulling blocks from height $lower"
        blocksPart <- blocksBufferR.getAndUpdate(_ - lower).map(_.get(lower))
        _ <- blocksPart match {
               case Some((List(best), others)) => syncQueue.enqueue1((best, others))
               case _                          => Timer[F].sleep(2.seconds) >> pullBlocks(lower, upper) // wait until block is available
             }
        numBlocks = blocksPart.map(_._2.size + 1).getOrElse(0)
        _ <- info"$numBlocks block(s) grabbed from height $lower"
        _ <- if (lower < upper) pullBlocks(lower + 1, upper) else unit[F]
      } yield ()

    private def index(best: ApiFullBlock, orphaned: List[ApiFullBlock]): F[Unit] = {
      val applyBlocks =
        for {
          _ <- info"Processing best block [${best.header.id}] at height [${best.header.height}]"
          _ <- info"Processing orphaned blocks [${orphaned.map(_.header.id).mkString(", ")}]"
          _ <- applyBestBlock(best)
          _ <- orphaned.traverse(applyOrphanedBlock)
          _ <- bestHeightR.set(Some(best.header.height))
        } yield ()
      applyBlocks.guarantee(commitChainUpdates)
    }

    private def applyBestBlock(block: ApiFullBlock): F[Unit] = {
      val id           = block.header.id
      val height       = block.header.height
      val parentId     = block.header.parentId
      val parentHeight = block.header.height - 1
      val checkParentF =
        getBlock(parentId).flatMap {
          case Some(parentBlock) if parentBlock.mainChain => unit[F]
          case None if block.header.height == startHeight => unit[F]
          case Some(parentBlock) =>
            info"Parent block [$parentId] needs to be updated" >> updateBestBlock(parentBlock)
          case None =>
            info"Parent block [$parentId] needs to be downloaded" >>
              network.getFullBlockById(parentId).flatMap {
                case Some(parentBlock) =>
                  applyBestBlock(parentBlock)
                case None =>
                  InconsistentNodeView(s"Failed to pull best block [$parentId] at height [$parentHeight]")
                    .raise[F, Unit]
              }
        }
      for {
        _             <- info"Applying best block [$id] at height [$height]"
        _             <- checkParentF
        prevBlockInfo <- getBlockInfo(parentId)
        flatBlock     <- scan(block, prevBlockInfo)
        _             <- insertBlock(flatBlock)
        _             <- markAsMain(id, height)
        _             <- memoize(flatBlock)
      } yield ()
    }

    private def memoize(fb: FlatBlock) =
      lastBlockCache.update { xs =>
        (fb.header, fb.info) :: xs.filter { case (header, _) => header.height >= fb.header.height - 1 }
      }

    private def applyOrphanedBlock(block: ApiFullBlock): F[Unit] =
      if (settings.writeOrphans)
        info"Applying orphaned block [${block.header.id}] at height [${block.header.height}]" >>
        getBlockInfo(block.header.parentId) >>= (scan(block, _) >>= insertBlock)
      else
        info"Skipping orphaned block [${block.header.id}] at height [${block.header.height}]"

    private def updateBestBlock(block: Header): F[Unit] = {
      val id       = block.id
      val height   = block.height
      val parentId = block.parentId
      info"Updating best block [$id] at height [$height]" >>
      getBlock(parentId).flatMap {
        case Some(parentBlock) if parentBlock.mainChain => unit[F]
        case None if block.height == startHeight        => unit[F]
        case Some(parentBlock) =>
          info"Parent block [$parentId] needs to be updated" >> updateBestBlock(parentBlock)
        case None =>
          info"Parent block [$parentId] needs to be downloaded" >>
            network.getFullBlockById(parentId).flatMap {
              case Some(parentBlock) => applyBestBlock(parentBlock).void
              case None =>
                val parentHeight = block.height - 1
                InconsistentNodeView(s"Failed to pull best block [$parentId] at height [$parentHeight]").raise[F, Unit]
            }
      } >> markAsMain(block.id, block.height) >> updateBlockInfo(block.parentId, block.id)
    }

    private def scan(apiFullBlock: ApiFullBlock, prevBlockInfoOpt: Option[BlockStats]): F[FlatBlock] = {
      implicit val ctx: F WithContext ProtocolSettings = Context.const(settings.protocol)
      SlotData(apiFullBlock, prevBlockInfoOpt).intoF[F, FlatBlock]
    }

    private def getLastGrabbedBlockHeight: F[Int] =
      bestHeightR.get.flatMap {
        case Some(height) => height.pure[F]
        case None         => repos.headers.getBestHeight ||> trans.xa
      }

    private def getHeaderIdsAtHeight(height: Int): D[List[BlockId]] =
      repos.headers.getAllByHeight(height).map(_.map(_.id))

    private def getBlock(id: BlockId): F[Option[Header]] =
      lastBlockCache.get.flatMap { xs =>
        xs.find { case (header, _) => header.id == id } match {
          case Some((h, _)) => h.some.pure[F]
          case None         => repos.headers.get(id) ||> trans.xa
        }
      }

    private def getBlockInfo(id: BlockId): F[Option[BlockStats]] =
      lastBlockCache.get.flatMap { xs =>
        xs.find { case (header, _) => header.id == id } match {
          case Some((_, bi))                        => bi.some.pure[F]
          case None if !settings.indexes.blockStats => none[BlockStats].pure[F]
          case None                                 => repos.blocksInfo.get(id) ||> trans.xa
        }
      }

    private def markAsMain(id: BlockId, height: Int): F[Unit] =
      pendingChainUpdates.update(_ :+ (id -> height))

    private def updateBlockInfo(prevBlockId: BlockId, blockId: BlockId): F[Unit] =
      (for {
        _                 <- OptionT.liftF(info"Updating stats of block $blockId")
        prevBlockStats    <- OptionT(getBlockInfo(prevBlockId))
        currentBlockStats <- OptionT(getBlockInfo(blockId))
        implicit0(ctx: WithContext[F, ProtocolSettings]) = Context.const[F, ProtocolSettings](settings.protocol)
        totalStats <- OptionT.liftF(blockStats.recalculateStats[F](currentBlockStats, prevBlockStats))
        _          <- OptionT.liftF(updateBlockStats(blockId, totalStats) ||> trans.xa)
      } yield ()).value.void

    private def commitChainUpdates: F[Unit] =
      pendingChainUpdates.get.flatMap { ids =>
        info"Updating ${ids.size} chain slots: [${ids.mkString(", ")}]" >>
        ids
          .traverse_ { case (id, height) =>
            getHeaderIdsAtHeight(height).flatMap { blocks =>
              val nonBest = blocks.filterNot(_ == id)
              updateChainStatus(id, mainChain = true) >> nonBest.traverse(updateChainStatus(_, mainChain = false))
            }
          }
          .thrushK(trans.xa)
          .flatMap(_ =>
            lastBlockCache.update {
              _.map {
                case (header, stats) if ids.exists(_._1 == header.id) => header.copy(mainChain = true) -> stats
                case (header, stats)                                  => header                        -> stats
              }
            }
          )
          .flatMap(_ => pendingChainUpdates.update(_ => List.empty))
      }

    private def updateBlockStats(
      blockId: BlockId,
      totals: TotalStats
    ): D[Unit] =
      repos.blocksInfo.updateTotalParamsByHeaderId(
        blockId,
        totals.blockChainTotalSize,
        totals.totalTxsCount,
        totals.totalMiningTime,
        totals.totalFees,
        totals.totalMinersReward,
        totals.totalCoinsInTxs
      )

    private def updateChainStatus(blockId: BlockId, mainChain: Boolean): D[Unit] =
      repos.headers.updateChainStatusById(blockId, mainChain) >>
      (if (settings.indexes.blockStats) repos.blocksInfo.updateChainStatusByHeaderId(blockId, mainChain)
       else unit[D]) >>
      repos.txs.updateChainStatusByHeaderId(blockId, mainChain) >>
      repos.outputs.updateChainStatusByHeaderId(blockId, mainChain) >>
      repos.inputs.updateChainStatusByHeaderId(blockId, mainChain) >>
      repos.dataInputs.updateChainStatusByHeaderId(blockId, mainChain)

    private def insertBlock(block: FlatBlock): F[Unit] = {
      val insertAll =
        repos.headers.insert(block.header) >>
        (if (settings.indexes.blockStats) repos.blocksInfo.insert(block.info) else unit[D]) >>
        (if (settings.indexes.blockExtensions) repos.blockExtensions.insert(block.extension) else unit[D]) >>
        (if (settings.indexes.adProofs) block.adProofOpt.map(repos.adProofs.insert).getOrElse(unit[D]) else unit[D]) >>
        repos.txs.insertMany(block.txs) >>
        repos.inputs.insetMany(block.inputs) >>
        repos.dataInputs.insetMany(block.dataInputs) >>
        repos.outputs.insertMany(block.outputs) >>
        repos.assets.insertMany(block.assets) >>
        (if (settings.indexes.boxRegisters) repos.registers.insertMany(block.registers) else unit[D]) >>
        repos.tokens.insertMany(block.tokens) >>
        (if (settings.indexes.boxRegisters) repos.constants.insertMany(block.constants) else unit[D])
      insertAll ||> trans.xa
    }
  }
}
