package org.ergoplatform.explorer.http.api.v1.services

import cats.Functor
import cats.effect.Sync
import mouse.anyf._
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories.EpochInfoRepo
import org.ergoplatform.explorer.http.api.v1.models.EpochInfo
import tofu.syntax.monadic._
import tofu.syntax.foption._

trait Epochs[F[_]] {

  /** Get data about last epoch.
    */
  def getLastEpoch: F[Option[EpochInfo]]
}

object Epochs {

  def apply[F[_]: Sync, D[_]: Functor: LiftConnectionIO](trans: D Trans F): F[Epochs[F]] =
    EpochInfoRepo[F, D].map(new Live[F, D](_)(trans))

  final private class Live[F[_], D[_]: Functor: LiftConnectionIO](epochInfoRepo: EpochInfoRepo[D])(trans: D Trans F)
    extends Epochs[F] {

    def getLastEpoch: F[Option[EpochInfo]] =
      epochInfoRepo.getLastEpoch.mapIn(EpochInfo(_)) ||> trans.xa
  }
}
