package org.ergoplatform.explorer.http.api.v1.services

import cats.effect.Sync
import org.ergoplatform.ErgoLikeContext.Height
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.EpochParameters
import org.ergoplatform.explorer.db.repositories.EpochInfoRepo
import mouse.anyf._
import org.ergoplatform.explorer.protocol.constants
import tofu.syntax.monadic._

trait Epochs[F[_]] {

  /** Get epoch params with the given `height`.
    */
  def getEpochParamsByHeight(height: Height): F[Option[EpochParameters]]

  /** Get epoch params with the given `id`.
    */
  def getEpochParamsById(id: Int): F[Option[EpochParameters]]
}

object Epochs {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO](trans: D Trans F): F[Epochs[F]] =
    EpochInfoRepo[F, D].map(new Live[F, D](_)(trans))

  final private class Live[F[_], D[_]: LiftConnectionIO](epochInfoRepo: EpochInfoRepo[D])(trans: D Trans F) extends Epochs[F] {

    override def getEpochParamsByHeight(height: Height): F[Option[EpochParameters]] =
      epochInfoRepo.getByEpochId((height) / constants.EpochLength) ||> trans.xa

    override def getEpochParamsById(id: Int): F[Option[EpochParameters]] =
      epochInfoRepo.getByEpochId(id) ||> trans.xa
  }
}
