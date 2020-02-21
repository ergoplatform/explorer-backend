package org.ergoplatform.explorer.http.api.v0.services

import cats.data.NonEmptyList
import cats.syntax.list._
import cats.{Monad, ~>}
import fs2.Stream
import org.ergoplatform.explorer.Err.RequestProcessingErr.InconsistentDbData
import org.ergoplatform.explorer.{CRaise, TokenId}
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.Paging
import org.ergoplatform.explorer.http.api.v0.models.OutputInfo
import org.ergoplatform.explorer.syntax.stream._
import tofu.syntax.raise._

/** A service providing an access to the assets data.
  */
trait AssetsService[F[_], S[_[_], _]] {

  /** Get boxes where given tokens where issued
    * according to EIP-4 https://github.com/ergoplatform/eips/blob/master/eip-0004.md
    */
  def getAllIssuingBoxes(paging: Paging): S[F, OutputInfo]

  /** Get boxes where given tokens where issued
    * according to EIP-4 https://github.com/ergoplatform/eips/blob/master/eip-0004.md
    */
  def getIssuingBoxes(tokenIds: NonEmptyList[TokenId]): S[F, OutputInfo]
}

object AssetsService {

  def apply[
    F[_],
    D[_]: LiftConnectionIO: CRaise[*[_], InconsistentDbData]: Monad
  ](
    xa: D ~> F
  ): AssetsService[F, Stream] =
    new Live(AssetRepo[D])(xa)

  final private class Live[
    F[_],
    D[_]: CRaise[*[_], InconsistentDbData]: Monad
  ](
    assetRepo: AssetRepo[D, Stream]
  )(xa: D ~> F)
    extends AssetsService[F, Stream] {

    def getAllIssuingBoxes(paging: Paging): Stream[F, OutputInfo] =
      (for {
        extOut <- assetRepo.getAllIssuingBoxes(paging.offset, paging.limit)
        assets <- assetRepo.getAllByBoxId(extOut.output.boxId).asStream
      } yield OutputInfo(extOut, assets)).translate(xa)

    def getIssuingBoxes(tokenIds: NonEmptyList[TokenId]): Stream[F, OutputInfo] =
      (for {
        extOuts <- assetRepo.getIssuingBoxesByTokenIds(tokenIds).asStream
        boxIdsNel <- extOuts
                      .map(_.output.boxId)
                      .toNel
                      .orRaise[D](InconsistentDbData("Empty outputs"))
                      .asStream
        assets <- assetRepo
                   .getAllByBoxIds(boxIdsNel)
                   .asStream
        outputInfo <- Stream.emits(OutputInfo.batch(extOuts, assets)).covary[D]
      } yield outputInfo).translate(xa)
  }
}
