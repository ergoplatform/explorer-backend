package org.ergoplatform.explorer.indexer.processes

import cats.effect.{Sync, Timer}
import cats.{Monad, Parallel}
import fs2.Stream
import io.scalaland.chimney.dsl._
import mouse.anyf._
import org.ergoplatform.explorer.services.ErgoNetwork
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.EpochParameters
import org.ergoplatform.explorer.indexer.modules.RepoBundle
import org.ergoplatform.explorer.protocol.constants
import org.ergoplatform.explorer.settings.IndexerSettings
import tofu.MonadThrow
import tofu.concurrent.MakeRef
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

trait EpochsIndexer[F[_]] {

  def run: Stream[F, Unit]
}

object EpochsIndexer {

  def apply[F[_]: Sync: Parallel: Timer, D[_]: MonadThrow: LiftConnectionIO](
    settings: IndexerSettings,
    network: ErgoNetwork[F]
  )(trans: Trans[D, F])(implicit logs: Logs[F, F], makeRef: MakeRef[F, F]): F[EpochsIndexer[F]] =
    logs.forService[EpochsIndexer[F]].flatMap { implicit log =>
      RepoBundle[F, D].map(new Live[F, D](settings, network, _)(trans))
    }

  final private class Live[F[_]: Logging: Monad: Timer, D[_]](
    settings: IndexerSettings,
    network: ErgoNetwork[F],
    repos: RepoBundle[D]
  )(
    trans: Trans[D, F]
  ) extends EpochsIndexer[F] {

    override def run: Stream[F, Unit] =
      Stream(()).repeat
        .covary[F]
        .metered(settings.pollInterval)
        .flatMap { _ =>
          Stream.eval(info"Starting epochs sync job ..") >> Stream.eval(sync).handleErrorWith { e =>
            Stream.eval(
              warnCause"An error occurred while syncing with the network. Restarting ..." (e)
            )
          }
        }

    def sync: F[Unit] =
      for {
        lastHeight  <- repos.epochInfoRepo.getLastHeight ||> trans.xa
        currentInfo <- network.getNodeInfo
        epochInfo   <- repos.epochInfoRepo.getByEpochId(lastHeight / constants.EpochLength) ||> trans.xa
        _ <- epochInfo match {
               case Some(_) => trace"Epoch info at height ${currentInfo.fullHeight} already persist"
               case None =>
                 repos.epochInfoRepo.insert(
                   currentInfo.parameters
                     .into[EpochParameters]
                     .withFieldComputed(_.id, _.height / constants.EpochLength)
                     .transform
                 ) ||> trans.xa
             }
      } yield ()
  }
}
