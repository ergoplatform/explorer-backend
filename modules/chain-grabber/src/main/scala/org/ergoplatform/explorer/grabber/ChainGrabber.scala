package org.ergoplatform.explorer.grabber

import java.util.concurrent.TimeUnit

import cats.effect.concurrent.Ref
import cats.effect.{Clock, Sync, Timer}
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.parallel._
import cats.syntax.traverse._
import cats.{Monad, MonadError, Parallel, ~>}
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monocle.macros.syntax.lens._
import mouse.anyf._
import org.ergoplatform.explorer.Err.{ProcessingErr, RefinementFailed}
import org.ergoplatform.explorer.clients.ergo.ErgoNetworkClient
import org.ergoplatform.explorer.{CRaise, Id}
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.BlockInfo
import org.ergoplatform.explorer.db.models.aggregates.FlatBlock
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.protocol.constants
import org.ergoplatform.explorer.protocol.models.ApiFullBlock
import org.ergoplatform.explorer.settings.GrabberAppSettings

/** Fetches new blocks from the network divide them into
  * separate entities and finally puts them into db.
  */
final class ChainGrabber[
  F[_]: Sync: Parallel: Logger: Timer,
  D[_]: CRaise[*[_], ProcessingErr]: CRaise[*[_], RefinementFailed]: Monad
](
   lastBlockCache: Ref[F, Option[BlockInfo]],
   settings: GrabberAppSettings,
   networkService: ErgoNetworkClient[F, Stream],
   headerRepo: HeaderRepo[D],
   blockInfoRepo: BlockInfoRepo[D, Stream],
   blockExtensionRepo: BlockExtensionRepo[D],
   adProofRepo: AdProofRepo[D],
   txRepo: TransactionRepo[D, Stream],
   inputRepo: InputRepo[D],
   outputRepo: OutputRepo[D, Stream],
   assetRepo: AssetRepo[D, Stream]
)(xa: D ~> F) {

  def run: Stream[F, Unit] =
    Stream(()).repeat
      .covary[F]
      .metered(settings.pollInterval)
      .evalMap { _ =>
        Logger[F].info("Starting sync job ..") >> grab.handleErrorWith { e =>
          Logger[F].warn(e)(
            "An error occurred while syncing with the network. Restarting ..."
          )
        }
      }

  private def grab: F[Unit] =
    for {
      networkHeight <- networkService.getBestHeight
      localHeight   <- getLastGrabbedBlockHeight
      _             <- Logger[F].info(s"Current network height : $networkHeight")
      _             <- Logger[F].info(s"Current explorer height: $localHeight")
      range         <- getScanRange(localHeight, networkHeight).pure[F]
      _ <- range.traverse { height =>
            grabBlocksFromHeight(height)
              .flatMap(_ ||> xa)
              .flatTap { blocks =>
                if (blocks.nonEmpty)
                  lastBlockCache.update(_ => blocks.headOption) >>
                  Logger[F].info(s"${blocks.size} block grabbed from height $height")
                else ProcessingErr.NoBlocksWritten(height = height).raiseError[F, Unit]
              }
          }
    } yield ()

  private def grabBlocksFromHeight(
    height: Int,
    existingHeaderIds: List[Id] = List.empty
  ): F[D[List[BlockInfo]]] =
    for {
      ids <- networkService.getBlockIdsAtHeight(height)
      apiBlocks <- ids
                    .filterNot(existingHeaderIds.contains)
                    .parTraverse(networkService.getFullBlockById)
                    .map {
                      _.flatten.map { block =>
                        block
                          .lens(_.header.mainChain)
                          .modify(_ => ids.headOption.contains(block.header.id))
                      }
                    }
      exStatuses = existingHeaderIds.map(id => id -> ids.headOption.contains(id))
      updatedForks <- exStatuses
                       .foldLeft(().pure[D]) {
                         case (acc, (id, status)) =>
                           acc >> headerRepo.updateChainStatusById(id, status)
                       }
                       .pure[F]
      blocks <- apiBlocks.parTraverse(processBlock).map(_.sequence)
    } yield updatedForks >> blocks

  private def processBlock(block: ApiFullBlock): F[D[BlockInfo]] =
    lastBlockCache.get.flatMap { cachedBlockOpt =>
      cachedBlockOpt
        .exists(_.headerId == block.header.id)
        .pure[F]
        .ifM(
          cachedBlockOpt.pure[F],
          getParentBlockInfo(block.header.parentId)
        )
        .flatMap {
          case None if block.header.height != constants.GenesisHeight => // fork
            getHeaderIdsAtHeight(block.header.height - 1).flatMap { existingHeaders =>
              grabBlocksFromHeight(block.header.height - 1, existingHeaders)
                .map(_.map(_.headOption))
            }
          case parentOpt =>
            parentOpt.pure[D].pure[F]
        }
        .flatMap { blockInfoOptDb =>
          Clock[F].realTime(TimeUnit.MILLISECONDS).map { ts =>
            blockInfoOptDb.flatMap { parentBlockInfoOpt =>
              FlatBlock
                .fromApi[D](block, parentBlockInfoOpt, ts)(settings.protocol)
                .flatMap(flatBlock => insetBlock(flatBlock) as flatBlock.info)
            }
          }
        }
    }

  private def getLastGrabbedBlockHeight: F[Int] =
    headerRepo.getBestHeight ||> xa

  private def getHeaderIdsAtHeight(height: Int): F[List[Id]] =
    (headerRepo.getAllByHeight(height) ||> xa).map(_.map(_.id))

  private def getParentBlockInfo(headerId: Id): F[Option[BlockInfo]] =
    blockInfoRepo.get(headerId) ||> xa

  private def getScanRange(localHeight: Int, networkHeight: Int): List[Int] =
    if (networkHeight == localHeight) List.empty
    else (localHeight + 1 to networkHeight).toList

  private def insetBlock(block: FlatBlock): D[Unit] =
    headerRepo.insert(block.header) >>
    blockInfoRepo.insert(block.info) >>
    blockExtensionRepo.insert(block.extension) >>
    block.adProofOpt.map(adProofRepo.insert).getOrElse(().pure[D]) >>
    txRepo.insertMany(block.txs) >>
    inputRepo.insetMany(block.inputs) >>
    outputRepo.insertMany(block.outputs) >>
    assetRepo.insertMany(block.assets)
}

object ChainGrabber {

  def apply[
    F[_]: Sync: Parallel: Timer,
    D[_]: LiftConnectionIO: MonadError[*[_], Throwable]
  ](
    settings: GrabberAppSettings,
    networkService: ErgoNetworkClient[F, Stream]
  )(xa: D ~> F): F[ChainGrabber[F, D]] =
    Slf4jLogger.create[F].flatMap { implicit logger =>
      Ref.of[F, Option[BlockInfo]](None).map { cache =>
        new ChainGrabber[F, D](
          cache,
          settings,
          networkService,
          HeaderRepo[D],
          BlockInfoRepo[D],
          BlockExtensionRepo[D],
          AdProofRepo[D],
          TransactionRepo[D],
          InputRepo[D],
          OutputRepo[D],
          AssetRepo[D]
        )(xa)
      }
    }
}
