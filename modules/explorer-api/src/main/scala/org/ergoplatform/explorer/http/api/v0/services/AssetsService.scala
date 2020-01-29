package org.ergoplatform.explorer.http.api.v0.services

import cats.effect.Sync
import cats.syntax.functor._
import cats.{~>, Monad}
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.explorer.Err.RequestProcessingErr.InconsistentDbData
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.v0.models.OutputInfo
import org.ergoplatform.explorer.syntax.stream._
import tofu.Raise.ContravariantRaise

/** A service providing an access to the assets data.
  */
trait AssetsService[F[_], S[_[_], _]] {

  /** Get boxes where given tokens where issued
    * according to EIP-4 https://github.com/ergoplatform/eips/blob/master/eip-0004.md
    */
  def getAllIssuingBoxes: S[F, OutputInfo]
}

object AssetsService {

  def apply[
    F[_]: Sync,
    D[_]: LiftConnectionIO: ContravariantRaise[*[_], InconsistentDbData]: Monad
  ](xa: D ~> F): F[AssetsService[F, Stream]] =
    Slf4jLogger
      .create[F]
      .map { implicit logger =>
        new Live(
          AssetRepo[D]
        )(xa)
      }

  final private class Live[
    F[_]: Sync: Logger,
    D[_]: ContravariantRaise[*[_], InconsistentDbData]: Monad
  ](
    assetRepo: AssetRepo[D, Stream]
  )(xa: D ~> F)
    extends AssetsService[F, Stream] {

    override def getAllIssuingBoxes: Stream[F, OutputInfo] =
      (for {
        extOut <- assetRepo.getAllIssuingBoxes
        assets <- assetRepo.getAllByBoxId(extOut.output.boxId).asStream
      } yield OutputInfo(extOut, assets)).translate(xa)

  }
}
