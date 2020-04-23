package org.ergoplatform.explorer.http.api.v0.services

import cats.Monad
import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.list._
import cats.syntax.functor._
import fs2.Stream
import mouse.anyf._
import org.ergoplatform.explorer.Err.RequestProcessingErr.InconsistentDbData
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.models.Paging
import org.ergoplatform.explorer.http.api.v0.models.OutputInfo
import org.ergoplatform.explorer.syntax.stream._
import org.ergoplatform.explorer.{CRaise, TokenId}
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

  /** Get total number of issuing boxes in the blockchain (number of assets).
    */
  def getIssuingBoxesQty: F[Int]
}

object AssetsService {

  def apply[
    F[_]: Sync,
    D[_]: LiftConnectionIO: CRaise[*[_], InconsistentDbData]: Monad
  ](trans: D Trans F): F[AssetsService[F, Stream]] =
    AssetRepo[F, D].map(new Live(_)(trans))

  final private class Live[
    F[_],
    D[_]: CRaise[*[_], InconsistentDbData]: Monad
  ](assetRepo: AssetRepo[D, Stream])(trans: D Trans F)
    extends AssetsService[F, Stream] {

    def getAllIssuingBoxes(paging: Paging): Stream[F, OutputInfo] =
      (for {
        extOut <- assetRepo.getAllIssuingBoxes(paging.offset, paging.limit)
        assets <- assetRepo.getAllByBoxId(extOut.output.boxId).asStream
      } yield OutputInfo(extOut, assets)) ||> trans.xas

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
      } yield outputInfo) ||> trans.xas

    def getIssuingBoxesQty: F[Int] =
      assetRepo.getIssuingBoxesQty ||> trans.xa
  }
}
