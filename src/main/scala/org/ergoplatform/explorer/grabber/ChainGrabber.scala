package org.ergoplatform.explorer.grabber

import cats.effect.concurrent.Ref
import cats.effect.{Sync, Timer}
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.parallel._
import cats.syntax.traverse._
import cats.{Applicative, Monad, Parallel}
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monocle.macros.syntax.lens._
import org.ergoplatform.explorer.db.models.BlockInfo
import org.ergoplatform.explorer.db.models.composite.FlatBlock
import org.ergoplatform.explorer.protocol.models.ApiFullBlock
import org.ergoplatform.explorer.services.{BlockchainService, ErgoNetworkService}
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.settings.Settings

final class ChainGrabber[F[_]: Monad: Parallel: Logger: Timer](
  lastBlockCache: Ref[F, Option[BlockInfo]],
  settings: Settings,
  blockchainService: BlockchainService[F],
  networkService: ErgoNetworkService[F, Stream[F, *]]
) {

  def run: Stream[F, Unit] =
    Stream(()).repeat
      .covary[F]
      .metered(settings.chainPollInterval)
      .evalMap(_ => grab)

  private def grab: F[Unit] =
    for {
      networkHeight <- networkService.getBestHeight
      localHeight   <- blockchainService.getBestHeight
      _             <- Logger[F].info(s"Current network height : $networkHeight")
      _             <- Logger[F].info(s"Current explorer height: $localHeight")
      range         <- getScanRange(localHeight, networkHeight).pure[F]
      _             <- range.traverse(grabBlocksFromHeight(_))
    } yield ()

  private def grabBlocksFromHeight(
    height: Int,
    existingHeaderIds: List[Id] = List.empty
  ): F[Unit] =
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
      exStatuses <- existingHeaderIds
                     .map(id => id -> ids.headOption.contains(id))
                     .pure[F]
      _ <- if (ids.size > existingHeaderIds.size) Applicative[F].unit // update
          else Applicative[F].unit
      blocks <- apiBlocks.map(processBlock).parSequence
    } yield ()

  private def processBlock(block: ApiFullBlock): F[BlockInfo] =
    for {
      cachedBlockOpt <- lastBlockCache.get
      parentOpt <- cachedBlockOpt
                    .exists(_.headerId == block.header.id)
                    .pure[F]
                    .ifM(
                      cachedBlockOpt.pure[F],
                      blockchainService.getBlockInfo(block.header.parentId)
                    )
      flatBlock <- FlatBlock.fromApi(block, parentOpt)(settings.protocol).pure[F]
    } yield ???

  private def getScanRange(localHeight: Int, networkHeight: Int): List[Int] =
    if (networkHeight == localHeight) List.empty
    else (localHeight + 1 to networkHeight).toList
}

object ChainGrabber {

  def apply[F[_]: Sync: Parallel: Timer](
    settings: Settings,
    blockchainService: BlockchainService[F],
    networkService: ErgoNetworkService[F, Stream[F, *]]
  ): F[ChainGrabber[F]] =
    Slf4jLogger.create[F].flatMap { implicit logger =>
      Ref.of[F, Option[BlockInfo]](None).map { cache =>
        new ChainGrabber[F](cache, settings, blockchainService, networkService)
      }
    }
}
