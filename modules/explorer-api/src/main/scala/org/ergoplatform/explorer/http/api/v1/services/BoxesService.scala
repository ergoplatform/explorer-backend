package org.ergoplatform.explorer.http.api.v1.services

import cats.{Functor, Monad}
import cats.effect.Sync
import cats.syntax.list._
import fs2.{Chunk, Pipe, Stream}
import mouse.anyf._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.{CRaise, TokenId}
import org.ergoplatform.explorer.Err.{RefinementFailed, RequestProcessingErr}
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.Output
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer.db.repositories.{AssetRepo, HeaderRepo, OutputRepo}
import org.ergoplatform.explorer.http.api.models.{Epochs, Items, Paging}
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v1.models.OutputInfo
import org.ergoplatform.explorer.settings.ServiceSettings
import org.ergoplatform.explorer.syntax.stream._
import tofu.streams.Compile
import tofu.syntax.streams.compile._
import tofu.syntax.monadic._
import tofu.fs2Instances._

trait BoxesService[F[_]] {

  /** Get all unspent outputs appeared in the blockchain after `minHeight`.
    */
  def getUnspentOutputs(epochs: Epochs): Stream[F, OutputInfo]

  /** Get all unspent outputs appeared in the blockchain within a given `lastEpochs`.
    */
  def getUnspentOutputs(lastEpochs: Int): Stream[F, OutputInfo]

  /** Get all outputs containing a given `tokenId`.
    */
  def getOutputsByTokenId(tokenId: TokenId, paging: Paging): F[Items[OutputInfo]]

  /** Get all unspent outputs containing a given `tokenId`.
    */
  def getUnspentOutputsByTokenId(tokenId: TokenId, paging: Paging): F[Items[OutputInfo]]
}

object BoxesService {

  def apply[
    F[_]: Sync,
    D[_]: CRaise[*[_], RequestProcessingErr]: CRaise[*[_], RefinementFailed]: LiftConnectionIO: Monad
  ](serviceSettings: ServiceSettings)(trans: D Trans F)(implicit e: ErgoAddressEncoder): F[BoxesService[F]] =
    (HeaderRepo[F, D], OutputRepo[F, D], AssetRepo[F, D]).mapN(new Live(serviceSettings, _, _, _)(trans))

  final private class Live[
    F[_]: Functor: CompileStream,
    D[_]: CRaise[*[_], RequestProcessingErr]: CRaise[*[_], RefinementFailed]: Monad
  ](
    serviceSettings: ServiceSettings,
    headerRepo: HeaderRepo[D],
    outRepo: OutputRepo[D, Stream],
    assetRepo: AssetRepo[D, Stream]
  )(trans: D Trans F)(implicit e: ErgoAddressEncoder)
    extends BoxesService[F] {

    def getUnspentOutputs(epochs: Epochs): Stream[F, OutputInfo] =
      outRepo
        .getAllMainUnspent(epochs.minHeight, epochs.maxHeight)
        .chunkN(serviceSettings.chunkSize)
        .through(toUnspentOutputInfo)
        .thrushK(trans.xas)

    def getUnspentOutputs(lastEpochs: Int): Stream[F, OutputInfo] =
      Stream
        .eval(headerRepo.getBestHeight)
        .flatMap { bestHeight =>
          outRepo.getAllMainUnspent(bestHeight - lastEpochs, bestHeight)
        }
        .chunkN(serviceSettings.chunkSize)
        .through(toUnspentOutputInfo)
        .thrushK(trans.xas)

    def getOutputsByTokenId(tokenId: TokenId, paging: Paging): F[Items[OutputInfo]] =
      outRepo
        .getAllByTokenId(tokenId, paging.offset, paging.limit)
        .chunkN(serviceSettings.chunkSize)
        .through(toOutputInfo)
        .thrushK(trans.xas)
        .to[List]
        .map(items => Items(items, items.size))

    def getUnspentOutputsByTokenId(tokenId: TokenId, paging: Paging): F[Items[OutputInfo]] =
      outRepo
        .getUnspentByTokenId(tokenId, paging.offset, paging.limit)
        .chunkN(serviceSettings.chunkSize)
        .through(toUnspentOutputInfo)
        .thrushK(trans.xas)
        .to[List]
        .map(items => Items(items, items.size))

    private def toOutputInfo: Pipe[D, Chunk[ExtendedOutput], OutputInfo] =
      for {
        outs   <- _
        outIds <- Stream.emit(outs.toList.map(_.output.boxId).toNel).unNone
        assets <- assetRepo.getAllByBoxIds(outIds).map(_.groupBy(_.boxId)).asStream
        outsInfo = outs.map(out => OutputInfo(out, assets.getOrElse(out.output.boxId, Nil)))
        flattened <- Stream.emits(outsInfo.toList)
      } yield flattened

    private def toUnspentOutputInfo: Pipe[D, Chunk[Output], OutputInfo] =
      for {
        outs   <- _
        outIds <- Stream.emit(outs.toList.map(_.boxId).toNel).unNone
        assets <- assetRepo.getAllByBoxIds(outIds).map(_.groupBy(_.boxId)).asStream
        outsInfo = outs.map(out => OutputInfo.unspent(out, assets.getOrElse(out.boxId, Nil)))
        flattened <- Stream.emits(outsInfo.toList)
      } yield flattened
  }
}
