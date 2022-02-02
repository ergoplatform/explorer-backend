package org.ergoplatform.explorer.http.api.v1.services

import cats.Monad
import cats.data.OptionT
import cats.effect.Sync
import mouse.anyf._
import org.ergoplatform.explorer.CRaise
import org.ergoplatform.explorer.Err.RequestProcessingErr.InconsistentDbData
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories.{BlockInfoRepo, EpochInfoRepo, StatsRepo}
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v1.models.{EpochInfo, NetworkState, NetworkStats}
import tofu.syntax.monadic._

trait Networks[F[_]] {

  def getState: F[Option[NetworkState]]

  def getStats: F[NetworkStats]
}

object Networks {

  def apply[
    F[_]: Sync,
    D[_]: Monad: LiftConnectionIO: CRaise[*[_], InconsistentDbData]: CompileStream
  ](trans: D Trans F): F[Networks[F]] =
    (BlockInfoRepo[F, D], EpochInfoRepo[F, D], StatsRepo[F, D]).mapN(new Live[F, D](_, _, _)(trans))

  final class Live[F[_], D[_]: Monad](blocks: BlockInfoRepo[D], epochs: EpochInfoRepo[D], stats: StatsRepo[D])(
    trans: D Trans F
  ) extends Networks[F] {

    def getState: F[Option[NetworkState]] = {
      val queryT =
        for {
          block  <- OptionT(blocks.getLastStats)
          params <- OptionT(epochs.getLastEpoch)
          epoch = EpochInfo(params)
        } yield NetworkState(block.headerId, block.height, block.maxBoxGix, block.maxTxGix, epoch)
      queryT.value ||> trans.xa
    }

    def getStats: F[NetworkStats] =
      stats.countUniqueAddrs.map(NetworkStats(_)) ||> trans.xa
  }
}
