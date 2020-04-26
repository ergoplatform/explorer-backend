package org.ergoplatform.explorer.grabber

import cats.effect.concurrent.Ref
import cats.effect.{Sync, Timer}
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.parallel._
import cats.syntax.traverse._
import cats.{Monad, MonadError, Parallel}
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monocle.macros.syntax.lens._
import mouse.anyf._
import org.ergoplatform.explorer.Err.{ProcessingErr, RefinementFailed}
import org.ergoplatform.explorer.context._
import org.ergoplatform.explorer.db.models.BlockInfo
import org.ergoplatform.explorer.db.models.aggregates.FlatBlock
import org.ergoplatform.explorer.protocol.constants
import org.ergoplatform.explorer.protocol.models.ApiFullBlock
import org.ergoplatform.explorer.{CRaise, Id, LiftConnectionIO}

/** Fetches new blocks from the network divide them into
  * separate entities and finally puts them into db.
  */
final class ChainGrabber[
  F[_]: Sync: Parallel: Logger: Timer,
  D[_]: CRaise[*[_], ProcessingErr]: CRaise[*[_], RefinementFailed]: Monad
](lastBlockCache: Ref[F, Option[BlockInfo]])(implicit F: HasGrabberContext[F, D]) {

  private val settings = implicitly[HasSettings[F]]
  private val repos    = implicitly[HasRepos[F, D]]

  def run: Stream[F, Unit] =
    Stream
      .eval(F.ask(_.settings.pollInterval))
      .flatMap { pollInterval =>
        Stream(()).repeat
          .covary[F]
          .metered(pollInterval)
          .evalMap { _ =>
            Logger[F].info("Starting sync job ..") >> grab.handleErrorWith { e =>
              Logger[F].warn(e)(
                "An error occurred while syncing with the network. Restarting ..."
              )
            }
          }
      }

  private def grab: F[Unit] =
    for {
      network       <- F.ask(_.networkClient)
      xa            <- F.ask(_.trans.xa)
      networkHeight <- network.getBestHeight
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
      network    <- F.ask(_.networkClient)
      headerRepo <- F.ask(_.repos.headerRepo)
      ids        <- network.getBlockIdsAtHeight(height)
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
    settings.askF { settings =>
      insertBlock.flatMap { insert =>
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
            .map { blockInfoOptDb =>
              blockInfoOptDb.flatMap { parentBlockInfoOpt =>
                FlatBlock
                  .fromApi[D](block, parentBlockInfoOpt)(settings.protocol)
                  .flatMap(flatBlock => insert(flatBlock) as flatBlock.info)
              }
            }
        }
      }
    }

  private def getLastGrabbedBlockHeight: F[Int] =
    F.askF(ctx => ctx.repos.headerRepo.getBestHeight ||> ctx.trans.xa)

  private def getHeaderIdsAtHeight(height: Int): F[List[Id]] =
    F.askF(ctx => (ctx.repos.headerRepo.getAllByHeight(height) ||> ctx.trans.xa).map(_.map(_.id)))

  private def getParentBlockInfo(headerId: Id): F[Option[BlockInfo]] =
    F.askF(ctx => ctx.repos.blockInfoRepo.get(headerId) ||> ctx.trans.xa)

  private def getScanRange(localHeight: Int, networkHeight: Int): List[Int] =
    if (networkHeight == localHeight) List.empty
    else (localHeight + 1 to networkHeight).toList

  private def insertBlock: F[FlatBlock => D[Unit]] =
    repos.ask { repos => block =>
      repos.headerRepo.insert(block.header) >>
      repos.blockInfoRepo.insert(block.info) >>
      repos.blockExtensionRepo.insert(block.extension) >>
      block.adProofOpt.map(repos.adProofsRepo.insert).getOrElse(().pure[D]) >>
      repos.transactionRepo.insertMany(block.txs) >>
      repos.inputRepo.insetMany(block.inputs) >>
      repos.outputRepo.insertMany(block.outputs) >>
      repos.assetRepo.insertMany(block.assets)
    }
}

object ChainGrabber {

  def apply[
    F[_]: Sync: Parallel: Timer,
    D[_]: LiftConnectionIO: MonadError[*[_], Throwable]
  ](implicit F: HasGrabberContext[F, D]): F[ChainGrabber[F, D]] =
    Slf4jLogger.create[F].flatMap { implicit logger =>
      Ref.of[F, Option[BlockInfo]](None).map(new ChainGrabber[F, D](_))
    }
}
