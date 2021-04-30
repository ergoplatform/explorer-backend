package org.ergoplatform.explorer.indexer.processes

import cats.data.OptionT
import cats.effect.concurrent.Ref
import cats.effect.syntax.bracket._
import cats.effect.{Bracket, Sync, Timer}
import cats.instances.list._
import cats.syntax.foldable._
import cats.syntax.parallel._
import cats.syntax.traverse._
import cats.{Monad, Parallel}
import fs2.Stream
import mouse.anyf._
import org.ergoplatform.explorer.BuildFrom.syntax._
import org.ergoplatform.explorer.Err.ProcessingErr.{InconsistentNodeView, NoBlocksWritten}
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.clients.ergo.ErgoNetworkClient
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.{BlockStats, Header}
import org.ergoplatform.explorer.indexer.extractors._
import org.ergoplatform.explorer.indexer.models.{FlatBlock, SlotData, TotalParams}
import org.ergoplatform.explorer.indexer.modules.RepoBundle
import org.ergoplatform.explorer.protocol.constants.GenesisHeight
import org.ergoplatform.explorer.protocol.models.ApiFullBlock
import org.ergoplatform.explorer.settings.{IndexerSettings, ProtocolSettings}
import tofu.concurrent.MakeRef
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.raise._
import tofu.{Context, MonadThrow, WithContext}

/** Synchronizes local view with the Ergo network.
  */
trait ChainIndexer[F[_]] {

  def run: Stream[F, Unit]
}

object ChainIndexer {

  def apply[F[_]: Sync: Parallel: Timer, D[_]: MonadThrow: LiftConnectionIO](
    settings: IndexerSettings,
    network: ErgoNetworkClient[F]
  )(trans: Trans[D, F])(implicit logs: Logs[F, F], makeRef: MakeRef[F, F]): F[ChainIndexer[F]] =
    logs.forService[ChainIndexer[F]].flatMap { implicit log =>
      makeRef.refOf(List.empty[(Id, Int)]).flatMap { updatesRef =>
        RepoBundle[F, D].map(new Live[F, D](settings, network, updatesRef, _)(trans))
      }
    }

  final class Live[
    F[_]: Monad: Parallel: Timer: Bracket[*[_], Throwable]: Logging,
    D[_]: Monad
  ](
    settings: IndexerSettings,
    network: ErgoNetworkClient[F],
    pendingChainUpdates: Ref[F, List[(Id, Int)]],
    repos: RepoBundle[D]
  )(trans: Trans[D, F])
    extends ChainIndexer[F] {

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
        _             <- Stream.eval(info"Current network height : $networkHeight")
        _             <- Stream.eval(info"Current explorer height: $localHeight")
        range = Stream.range(localHeight + 1, networkHeight + 1)
        _ <- range.evalMap { height =>
               index(height)
                 .flatTap { blocks =>
                   if (blocks > 0) info"$blocks block(s) grabbed from height $height"
                   else NoBlocksWritten(height).raise[F, Unit]
                 }
             }
      } yield ()

    private def index(height: Int): F[Int] = {
      val pullBlocks =
        for {
          ids    <- network.getBlockIdsAtHeight(height)
          _      <- info"Grabbing blocks at height $height: [${ids.mkString(",")}]"
          blocks <- ids.parTraverse(network.getFullBlockById)
          (best, orphaned) = blocks.flatten.partition(b => ids.headOption.contains(b.header.id))
          _ <- info"Best block [${best.headOption.map(_.header.id)}]"
          _ <- info"Orphaned blocks [${orphaned.map(_.header.id).mkString(", ")}]"
          _ <- best.traverse(applyBestBlock)
          _ <- orphaned.traverse(applyOrphanedBlock)
        } yield blocks.size
      pullBlocks.guarantee(commitChainUpdates)
    }

    private def applyBestBlock(block: ApiFullBlock): F[Unit] = {
      val id           = block.header.id
      val height       = block.header.height
      val parentId     = block.header.parentId
      val parentHeight = block.header.height - 1
      val checkParentF =
        getBlock(parentId).flatMap {
          case Some(parentBlock) if parentBlock.mainChain   => unit[F]
          case None if block.header.height == GenesisHeight => unit[F]
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
      info"Applying best block [$id] at height [$height]" >>
      checkParentF >> getBlockInfo(parentId) >>= (scan(block, _)) >>= insertBlock >>= (_ =>
        markAsMain(id, height) >> updateBlockInfo(parentId, id)
      )
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
        case None if block.height == GenesisHeight      => unit[F]
        case Some(parentBlock) =>
          info"Parent block [$parentId] needs to be updated" >> updateBestBlock(parentBlock)
        case None =>
          info"Parent block [$parentId] needs to be downloaded" >>
            network.getFullBlockById(parentId).flatMap {
              case Some(parentBlock) => applyBestBlock(parentBlock)
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
      repos.headers.getBestHeight ||> trans.xa

    private def getHeaderIdsAtHeight(height: Int): D[List[Id]] =
      repos.headers.getAllByHeight(height).map(_.map(_.id))

    private def getBlock(id: Id): F[Option[Header]] =
      repos.headers.get(id) ||> trans.xa

    private def getBlockInfo(id: Id): F[Option[BlockStats]] =
      repos.blocksInfo.get(id) ||> trans.xa

    private def markAsMain(id: Id, height: Int): F[Unit] =
      pendingChainUpdates.update(_ :+ (id -> height))

    private def updateBlockInfo(prevBlockId: Id, blockId: Id): F[Unit] =
      (for {
        prevBlockStats    <- OptionT(getBlockInfo(prevBlockId))
        currentBlockStats <- OptionT(getBlockInfo(blockId))
        implicit0(ctx: WithContext[F, ProtocolSettings]) = Context.const[F, ProtocolSettings](settings.protocol)
        totalParams <- OptionT.liftF(blockStats.produceTotalParams[F](currentBlockStats, prevBlockStats))
        _           <- OptionT.liftF[F, Unit](updateBlockInfoStats(blockId, totalParams).thrushK(trans.xa))
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
          .flatMap(_ => pendingChainUpdates.update(_ => List.empty))
      }

    private def updateBlockInfoStats(
      headerId: Id,
      totalParams: TotalParams
    ): D[Unit] =
      repos.blocksInfo.updateTotalParamsByHeaderId(
        headerId,
        totalParams.blockChainTotalSize,
        totalParams.totalTxsCount,
        totalParams.totalMiningTime,
        totalParams.totalFees,
        totalParams.totalMinersReward,
        totalParams.totalCoinsInTxs
      )

    private def updateChainStatus(headerId: Id, mainChain: Boolean): D[Unit] =
      repos.headers.updateChainStatusById(headerId, mainChain) >>
      repos.blocksInfo.updateChainStatusByHeaderId(headerId, mainChain) >>
      repos.txs.updateChainStatusByHeaderId(headerId, mainChain) >>
      repos.outputs.updateChainStatusByHeaderId(headerId, mainChain) >>
      repos.inputs.updateChainStatusByHeaderId(headerId, mainChain) >>
      repos.dataInputs.updateChainStatusByHeaderId(headerId, mainChain)

    private def insertBlock(block: FlatBlock): F[Unit] = {
      val insertAll =
        repos.headers.insert(block.header) >>
        repos.blocksInfo.insert(block.info) >>
        repos.blockExtensions.insert(block.extension) >>
        block.adProofOpt.map(repos.adProofs.insert).getOrElse(unit[D]) >>
        repos.txs.insertMany(block.txs) >>
        repos.inputs.insetMany(block.inputs) >>
        repos.dataInputs.insetMany(block.dataInputs) >>
        repos.outputs.insertMany(block.outputs) >>
        repos.assets.insertMany(block.assets) >>
        repos.registers.insertMany(block.registers) >>
        repos.tokens.insertMany(block.tokens) >>
        repos.constants.insertMany(block.constants)
      insertAll ||> trans.xa
    }
  }
}
