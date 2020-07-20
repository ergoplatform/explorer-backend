package org.ergoplatform.explorer.grabber.services

import cats.effect.concurrent.Ref
import cats.effect.{Sync, Timer}
import cats.instances.list._
import cats.syntax.foldable._
import cats.syntax.parallel._
import cats.syntax.traverse._
import cats.{~>, Monad, MonadError, Parallel}
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monocle.macros.syntax.lens._
import mouse.anyf._
import org.ergoplatform.explorer.Err.{ProcessingErr, RefinementFailed}
import org.ergoplatform.explorer.clients.ergo.ErgoNetworkClient
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.BlockInfo
import org.ergoplatform.explorer.db.models.aggregates.FlatBlock
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.protocol.constants._
import org.ergoplatform.explorer.protocol.models.ApiFullBlock
import org.ergoplatform.explorer.settings.ProtocolSettings
import org.ergoplatform.explorer.{CRaise, Id}
import tofu.syntax.monadic._
import tofu.syntax.raise._

trait GrabberService[F[_]] {

  /** Sync all known blocks in the network.
    */
  def syncAll: Stream[F, Unit]
}

object GrabberService {

  def apply[
    F[_]: Sync: Parallel: Timer,
    D[_]: LiftConnectionIO: MonadError[*[_], Throwable]
  ](
    settings: ProtocolSettings,
    network: ErgoNetworkClient[F]
  )(xa: D ~> F): F[GrabberService[F]] =
    Slf4jLogger.create[F].flatMap { implicit logger =>
      Ref.of[F, Option[BlockInfo]](None).flatMap { cache =>
        (
          HeaderRepo[F, D],
          BlockInfoRepo[F, D],
          BlockExtensionRepo[F, D],
          AdProofRepo[F, D],
          TransactionRepo[F, D],
          InputRepo[F, D],
          OutputRepo[F, D],
          AssetRepo[F, D]
        ).mapN(new Live[F, D](cache, settings, network, _, _, _, _, _, _, _, _)(xa))
      }
    }

  final class Live[
    F[_]: Sync: Parallel: Logger: Timer,
    D[_]: CRaise[*[_], ProcessingErr]: CRaise[*[_], RefinementFailed]: Monad
  ](
    lastBlockCache: Ref[F, Option[BlockInfo]],
    settings: ProtocolSettings,
    network: ErgoNetworkClient[F],
    headerRepo: HeaderRepo[D],
    blockInfoRepo: BlockInfoRepo[D],
    blockExtensionRepo: BlockExtensionRepo[D],
    adProofRepo: AdProofRepo[D],
    txRepo: TransactionRepo[D, Stream],
    inputRepo: InputRepo[D],
    outputRepo: OutputRepo[D, Stream],
    assetRepo: AssetRepo[D, Stream]
  )(xa: D ~> F)
    extends GrabberService[F] {

    private val log = Logger[F]

    def syncAll: Stream[F, Unit] =
      for {
        networkHeight <- Stream.eval(network.getBestHeight)
        localHeight   <- Stream.eval(getLastGrabbedBlockHeight)
        _             <- Stream.eval(log.info(s"Current network height : $networkHeight"))
        _             <- Stream.eval(log.info(s"Current explorer height: $localHeight"))
        range = Stream.range(localHeight + 1, networkHeight + 1)
        _ <- range.evalMap { height =>
               grabBlocksFromHeight(height)
                 .flatMap(_ ||> xa)
                 .flatTap { blocks =>
                   if (blocks.nonEmpty)
                     lastBlockCache.update(_ => blocks.headOption) >>
                     log.info(s"${blocks.size} block(s) grabbed from height [$height]")
                   else ProcessingErr.NoBlocksWritten(height = height).raise[F, Unit]
                 }
             }
      } yield ()

    private def grabBlocksFromHeight(
      height: Int,
      existingHeaderIds: List[Id] = List.empty
    ): F[D[List[BlockInfo]]] =
      for {
        ids <- network.getBlockIdsAtHeight(height)
        _   <- log.debug(s"Grabbing blocks height $height: [${ids.mkString(",")}]")
        _   <- log.debug(s"Known blocks: [${existingHeaderIds.mkString(",")}]")
        apiBlocks <- ids
                       .filterNot(existingHeaderIds.contains)
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
        exStatuses  = existingHeaderIds.map(id => id -> ids.headOption.contains(id))
        updateForks = exStatuses.traverse_ { case (id, status) => updateChainStatus(id, status) }
        _ <- log.debug(s"Updated statuses at height $height: [${exStatuses.map(x => s"[${x._1}](main=${x._2})").mkString(",")}]")
        blocks <- apiBlocks.sortBy(_.header.mainChain).traverse(processBlock).map(_.sequence)
      } yield updateForks >> blocks

    private def processBlock(block: ApiFullBlock): F[D[BlockInfo]] =
      log.info(s"Processing full block ${block.header.id}") >>
      lastBlockCache.get.flatMap { cachedBlockOpt =>
        val isCached   = cachedBlockOpt.exists(_.headerId == block.header.parentId)
        val parentOptF = if (isCached) cachedBlockOpt.pure[F] else getParentBlockInfo(block.header.parentId)
        parentOptF
          .flatMap {
            case None if block.header.height != GenesisHeight && block.header.mainChain => // fork
              val forkHeight = block.header.height - 1
              log.info(s"Processing fork at height $forkHeight") >>
              getHeaderIdsAtHeight(forkHeight).flatMap { existingHeaders =>
                grabBlocksFromHeight(forkHeight, existingHeaders)
                  .map(_.map(_.headOption))
              }
            case parentOpt =>
              log.debug(s"Parent is ${parentOpt.map(_.headerId).getOrElse("<not found>")}") >>
              parentOpt.pure[D].pure[F]
          }
          .map { blockInfoOptDb =>
            blockInfoOptDb.flatMap { parentBlockInfoOpt =>
              FlatBlock
                .fromApi[D](block, parentBlockInfoOpt)(settings)
                .flatMap(flatBlock => insertBlock(flatBlock) as flatBlock.info)
            }
          }
      }

    private def getLastGrabbedBlockHeight: F[Int] =
      headerRepo.getBestHeight ||> xa

    private def getHeaderIdsAtHeight(height: Int): F[List[Id]] =
      (headerRepo.getAllByHeight(height) ||> xa).map(_.map(_.id))

    private def getParentBlockInfo(headerId: Id): F[Option[BlockInfo]] =
      blockInfoRepo.get(headerId) ||> xa

    private def updateChainStatus(headerId: Id, newChainStatus: Boolean): D[Unit] =
      headerRepo.updateChainStatusById(headerId, newChainStatus) >>
      txRepo.updateChainStatusByHeaderId(headerId, newChainStatus) >>
      outputRepo.updateChainStatusByHeaderId(headerId, newChainStatus) >>
      inputRepo.updateChainStatusByHeaderId(headerId, newChainStatus)

    private def insertBlock(block: FlatBlock): D[Unit] =
      headerRepo.insert(block.header) >>
      blockInfoRepo.insert(block.info) >>
      blockExtensionRepo.insert(block.extension) >>
      block.adProofOpt.map(adProofRepo.insert).getOrElse(().pure[D]) >>
      txRepo.insertMany(block.txs) >>
      inputRepo.insetMany(block.inputs) >>
      outputRepo.insertMany(block.outputs) >>
      assetRepo.insertMany(block.assets)
  }
}
