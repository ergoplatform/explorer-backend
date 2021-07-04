package org.ergoplatform.explorer.http.api.v1.services

import cats.Monad
import cats.data.OptionT
import cats.effect.Sync
import mouse.anyf._
import org.ergoplatform.explorer.CRaise
import org.ergoplatform.explorer.Err.RequestProcessingErr.InconsistentDbData
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories.{BlockInfoRepo, EpochInfoRepo}
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v1.models.NetworkInfo
import tofu.syntax.monadic._

trait NetworkInfos[F[_]] {

  def getNetworkInfo: F[Option[NetworkInfo]]
}

object NetworkInfos {

  def apply[
    F[_]: Sync,
    D[_]: Monad: LiftConnectionIO: CRaise[*[_], InconsistentDbData]: CompileStream
  ](trans: D Trans F): F[NetworkInfos[F]] =
    (BlockInfoRepo[F, D], EpochInfoRepo[F, D]).mapN(new Live[F, D](_, _)(trans))

  final class Live[F[_], D[_]: Monad](blocks: BlockInfoRepo[D], epochs: EpochInfoRepo[D])(trans: D Trans F)
    extends NetworkInfos[F] {

    def getNetworkInfo: F[Option[NetworkInfo]] = {
      val queryT =
        for {
          block <- OptionT(blocks.getLastStats)
          epoch <- OptionT(epochs.getLastEpoch)
        } yield NetworkInfo(block.headerId, block.height, block.maxTxGix, block.maxBoxGix, epoch)
      queryT.value ||> trans.xa
    }
  }
}
