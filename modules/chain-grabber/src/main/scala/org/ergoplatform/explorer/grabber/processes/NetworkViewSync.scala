package org.ergoplatform.explorer.grabber.processes

import cats.effect.concurrent.Ref
import cats.effect.{Sync, Timer}
import cats.instances.list._
import cats.syntax.foldable._
import cats.syntax.option._
import cats.syntax.parallel._
import cats.syntax.traverse._
import cats.{~>, Monad, Parallel}
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monocle.macros.syntax.lens._
import mouse.anyf._
import org.ergoplatform.explorer.Err.ProcessingErr
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.clients.ergo.ErgoNetworkClient
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.BlockInfo
import org.ergoplatform.explorer.grabber.extractors._
import org.ergoplatform.explorer.grabber.models.{FlatBlock, SlotData}
import org.ergoplatform.explorer.grabber.modules.BuildFrom.syntax._
import org.ergoplatform.explorer.grabber.modules.RepoBundle
import org.ergoplatform.explorer.protocol.constants._
import org.ergoplatform.explorer.protocol.models.ApiFullBlock
import org.ergoplatform.explorer.settings.ProtocolSettings
import tofu.syntax.monadic._
import tofu.syntax.raise._
import tofu.{Context, Raise, Throws, WithContext}

trait NetworkViewSync[F[_]] {

  /** Sync local view with the network.
    */
  def run: Stream[F, Unit]
}

object NetworkViewSync {

  def apply[
    F[_]: Sync: Parallel: Timer,
    D[_]: LiftConnectionIO: Throws: Monad
  ](
    settings: ProtocolSettings,
    network: ErgoNetworkClient[F]
  )(xa: D ~> F): F[NetworkViewSync[F]] =
    Slf4jLogger.create[F].flatMap { implicit logger =>
      Ref.of[F, Option[BlockInfo]](None).flatMap { cache =>
        RepoBundle[F, D].map(new Live[F, D](cache, settings, network, _)(xa))
      }
    }

  final class Live[
    F[_]: Monad: Parallel: Logger: Timer: Raise[*[_], ProcessingErr],
    D[_]: Monad: Throws
  ](
    lastBlockCache: Ref[F, Option[BlockInfo]],
    settings: ProtocolSettings,
    network: ErgoNetworkClient[F],
    repos: RepoBundle[D]
  )(xa: D ~> F)
    extends NetworkViewSync[F] {

    private val log = Logger[F]

    def run: Stream[F, Unit] =
      for {
        networkHeight <- Stream.eval(network.getBestHeight)
        localHeight   <- Stream.eval(getLastGrabbedBlockHeight)
        _             <- Stream.eval(log.info(s"Current network height : $networkHeight"))
        _             <- Stream.eval(log.info(s"Current explorer height: $localHeight"))
        range = Stream.range(localHeight + 1, networkHeight + 1)
        _ <- range.evalMap { height =>
               scanHeight(height)
                 .flatMap(_ ||> xa)
                 .flatTap { blocks =>
                   if (blocks.nonEmpty)
                     lastBlockCache.update(_ => blocks.find(_.mainChain)) >>
                     log.info(s"${blocks.size} block(s) grabbed from height $height")
                   else ProcessingErr.NoBlocksWritten(height = height).raise[F, Unit]
                 }
             }
      } yield ()

    private def scanHeight(height: Int): F[D[List[BlockInfo]]] =
      for {
        ids         <- network.getBlockIdsAtHeight(height)
        existingIds <- getHeaderIdsAtHeight(height)
        _           <- log.debug(s"Grabbing blocks at height $height: [${ids.mkString(",")}]")
        _           <- log.debug(s"Known blocks: [${existingIds.mkString(",")}]")
        apiBlocks <- ids
                       .filterNot(existingIds.contains)
                       .parTraverse(network.getFullBlockById)
                       .map {
                         _.flatten.map { block =>
                           block
                             .lens(_.header.mainChain)
                             .modify(_ => ids.headOption.contains(block.header.id))
                         }
                       }
                       .flatTap { bs =>
                         log.debug(s"Got [${bs.size}] full blocks: [${bs.map(_.header.id).mkString(",")}]")
                       }
        exStatuses  = existingIds.map(id => id -> ids.headOption.contains(id))
        updateForks = exStatuses.traverse_ { case (id, status) => updateChainStatus(id, status) }
        _ <-
          log.debug(
            s"Updating statuses at height $height: [${exStatuses.map(x => s"[${x._1}](main=${x._2})").mkString(",")}]"
          )
        blocks <- apiBlocks.sortBy(x => !x.header.mainChain).traverse(processBlock).map(_.sequence)
      } yield updateForks >> blocks

    private def processBlock(block: ApiFullBlock): F[D[BlockInfo]] = {
      val blockId    = block.header.id
      val parentId   = block.header.parentId
      val prevHeight = block.header.height - 1
      val processF =
        lastBlockCache.get.flatMap { cachedBlockOpt =>
          val isCached   = cachedBlockOpt.exists(_.headerId == parentId)
          val parentOptF = if (isCached) cachedBlockOpt.pure[F] else getBlockInfo(parentId)
          log.debug(s"Cached block: ${cachedBlockOpt.map(_.headerId).getOrElse("<none>")}") >>
          parentOptF
            .flatMap {
              case None if block.header.height != GenesisHeight =>
                log.info(s"Processing unknown fork at height $prevHeight") >>
                  scanHeight(prevHeight).map(_.map(_.headOption))
              case Some(parent) if block.header.mainChain && !parent.mainChain =>
                log.info(s"Processing fork at height $prevHeight") >>
                  scanHeight(prevHeight).map(_.map(_ => parent.copy(mainChain = true).some))
              case parentOpt =>
                log.debug(s"Parent block: ${parentOpt.map(_.headerId).getOrElse("<none>")}") >>
                  parentOpt.pure[D].pure[F]
            }
            .map { blockInfoOptDb =>
              blockInfoOptDb.flatMap { parentBlockInfoOpt =>
                scan(block, parentBlockInfoOpt)
                  .flatMap(flatBlock => insertBlock(flatBlock) as flatBlock.info)
              }
            }
        }
      log.info(s"Processing full block $blockId") >>
      getBlockInfo(block.header.id).flatMap { // out of tx!
        case None        => processF
        case Some(block) => log.warn(s"Block [$blockId] already written") as block.pure[D]
      }
    }

    private def scan(apiFullBlock: ApiFullBlock, prevBlockInfoOpt: Option[BlockInfo]): D[FlatBlock] = {
      implicit val ctx: WithContext[D, ProtocolSettings] = Context.const(settings)
      SlotData(apiFullBlock, prevBlockInfoOpt).intoF[D, FlatBlock]
    }

    private def getLastGrabbedBlockHeight: F[Int] =
      repos.headers.getBestHeight.thrushK(xa)

    private def getHeaderIdsAtHeight(height: Int): F[List[Id]] =
      repos.headers.getAllByHeight(height).thrushK(xa).map(_.map(_.id))

    private def getBlockInfo(headerId: Id): F[Option[BlockInfo]] =
      repos.blocksInfo.get(headerId).thrushK(xa)

    private def updateChainStatus(headerId: Id, newChainStatus: Boolean): D[Unit] =
      repos.headers.updateChainStatusById(headerId, newChainStatus) >>
      repos.blocksInfo.updateChainStatusByHeaderId(headerId, newChainStatus) >>
      repos.txs.updateChainStatusByHeaderId(headerId, newChainStatus) >>
      repos.outputs.updateChainStatusByHeaderId(headerId, newChainStatus) >>
      repos.inputs.updateChainStatusByHeaderId(headerId, newChainStatus) >>
      repos.dataInputs.updateChainStatusByHeaderId(headerId, newChainStatus)

    private def insertBlock(block: FlatBlock): D[Unit] =
      repos.headers.insert(block.header) >>
      repos.blocksInfo.insert(block.info) >>
      repos.blockExtensions.insert(block.extension) >>
      block.adProofOpt.map(repos.adProofs.insert).getOrElse(().pure[D]) >>
      repos.txs.insertMany(block.txs) >>
      repos.inputs.insetMany(block.inputs) >>
      repos.dataInputs.insetMany(block.dataInputs) >>
      repos.outputs.insertMany(block.outputs) >>
      repos.assets.insertMany(block.assets) >>
      repos.registers.insertMany(block.registers) >>
      repos.tokens.insertMany(block.tokens)
  }
}
