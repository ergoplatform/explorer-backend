package org.ergoplatform.explorer.http.api.v1.services

import cats.data.{NonEmptyList, OptionT}
import cats.effect.Sync
import cats.syntax.semigroupk._
import cats.syntax.list._
import cats.{Functor, Monad}
import fs2.{Chunk, Pipe, Stream}
import mouse.all._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.Err.{RefinementFailed, RequestProcessingErr}
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.{AnyOutput, Output, UOutput}
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedOutput, ExtendedUOutput}
import org.ergoplatform.explorer.db.repositories.{
  AssetRepo,
  HeaderRepo,
  OutputRepo,
  UAssetRepo,
  UInputRepo,
  UOutputRepo
}
import org.ergoplatform.explorer.http.api.models.Sorting.SortOrder
import org.ergoplatform.explorer.http.api.models.{HeightRange, Items, Paging}
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v1.models.{
  AnyOutputInfo,
  BoxAssetsQuery,
  BoxQuery,
  MOutputInfo,
  OutputInfo,
  UOutputInfo
}
import org.ergoplatform.explorer.http.api.v1.shared.MempoolProps
import org.ergoplatform.explorer.protocol.sigma._
import org.ergoplatform.explorer.settings.ServiceSettings
import org.ergoplatform.explorer.syntax.stream._
import tofu.Throws
import tofu.fs2Instances._
import tofu.syntax.monadic._
import tofu.syntax.streams.compile._

trait Boxes[F[_]] {

  /** Get output by `boxId`.
    */
  def getOutputById(id: BoxId): F[Option[OutputInfo]]

  /** Get all outputs with the given `address` in proposition.
    */
  def getOutputsByAddress(address: Address, paging: Paging): F[Items[OutputInfo]]

  def `getUnspent&UnconfirmedOutputsMergedByAddress`(
    address: Address,
    ord: SortOrder
  ): F[List[MOutputInfo]]

  /** Get unspent outputs with the given `address` in proposition.
    */
  def getUnspentOutputsByAddress(address: Address, paging: Paging, ord: SortOrder): F[Items[OutputInfo]]

  /** Get all outputs with the given `ergoTree` in proposition.
    */
  def getOutputsByErgoTree(ergoTree: HexString, paging: Paging): F[Items[OutputInfo]]

  /** Get unspent outputs with the given `ergoTree` in proposition.
    */
  def getUnspentOutputsByErgoTree(ergoTree: HexString, paging: Paging, ord: SortOrder): F[Items[OutputInfo]]

  /** Get all outputs containing a given `tokenId`.
    */
  def getOutputsByErgoTreeTemplateHash(template: ErgoTreeTemplateHash, paging: Paging): F[Items[OutputInfo]]

  /** Get all unspent outputs containing a given `tokenId`.
    */
  def getUnspentOutputsByErgoTreeTemplateHash(template: ErgoTreeTemplateHash, paging: Paging): F[Items[OutputInfo]]

  /** Get all outputs containing a given `tokenId`.
    */
  def streamOutputsByErgoTreeTemplateHash(template: ErgoTreeTemplateHash, range: HeightRange): Stream[F, OutputInfo]

  /** Get all unspent outputs containing a given `tokenId`.
    */
  def streamUnspentOutputsByErgoTreeTemplateHash(
    template: ErgoTreeTemplateHash,
    range: HeightRange
  ): Stream[F, OutputInfo]

  /** Get all unspent outputs appeared in the blockchain after `minHeight`.
    */
  def streamUnspentOutputs(range: HeightRange): Stream[F, OutputInfo]

  /** Get all unspent outputs appeared in the blockchain within a suffix of `suffixLen`.
    */
  def streamUnspentOutputs(suffixLen: Int): Stream[F, OutputInfo]

  /** Get all unspent outputs appeared in the blockchain after an output at a given global index `minGix` (inclusively).
    */
  def streamUnspentOutputs(minGix: Long, limit: Int): Stream[F, OutputInfo]

  /** Get all unspent outputs appeared in the blockchain after an output at a given global index `minGix` (inclusively).
    */
  def streamOutputs(minGix: Long, limit: Int): Stream[F, OutputInfo]

  /** Get all outputs containing a given `tokenId`.
    */
  def getOutputsByTokenId(tokenId: TokenId, paging: Paging): F[Items[OutputInfo]]

  /** Get all unspent outputs containing a given `tokenId`.
    */
  def getUnspentOutputsByTokenId(tokenId: TokenId, paging: Paging, ord: SortOrder): F[Items[OutputInfo]]

  /** Get all outputs matching a given `boxQuery`.
    */
  def searchAll(boxQuery: BoxQuery, paging: Paging): F[Items[OutputInfo]]

  /** Get unspent outputs matching a given `boxQuery`.
    */
  def searchUnspent(boxQuery: BoxQuery, paging: Paging): F[Items[OutputInfo]]

  /** Get unspent outputs matching a given `boxQuery`.
    */
  def searchUnspentByAssetsUnion(boxQuery: BoxAssetsQuery, paging: Paging): F[Items[OutputInfo]]

  /** Get both confirmed & unconfirmed outputs with the given `address` in proposition.
    */
  def getAllUnspentOutputs(address: Address, paging: Paging, ord: SortOrder): F[Items[AnyOutputInfo]]
}

object Boxes {

  def apply[
    F[_]: Sync,
    D[_]: Monad: Throws: LiftConnectionIO: CompileStream
  ](serviceSettings: ServiceSettings, memprops: MempoolProps[F, D])(trans: D Trans F)(implicit
    e: ErgoAddressEncoder
  ): F[Boxes[F]] =
    (HeaderRepo[F, D], OutputRepo[F, D], AssetRepo[F, D], UAssetRepo[F, D], UOutputRepo[F, D], UInputRepo[F, D]).mapN(
      new Live(serviceSettings, memprops, _, _, _, _, _, _)(trans)
    )

  final private class Live[
    F[_]: Monad: CompileStream,
    D[_]: Monad: CRaise[*[_], RequestProcessingErr]: CRaise[*[_], RefinementFailed]: CompileStream
  ](
    serviceSettings: ServiceSettings,
    memprops: MempoolProps[F, D],
    headers: HeaderRepo[D, Stream],
    outputs: OutputRepo[D, Stream],
    assets: AssetRepo[D, Stream],
    uassets: UAssetRepo[D],
    uoutputs: UOutputRepo[D, Stream],
    uinputs: UInputRepo[D, Stream]
  )(trans: D Trans F)(implicit e: ErgoAddressEncoder)
    extends Boxes[F] {

    def getOutputById(id: BoxId): F[Option[OutputInfo]] =
      (for {
        box    <- OptionT(outputs.getByBoxId(id))
        assets <- OptionT.liftF(assets.getAllByBoxId(box.output.boxId))
      } yield OutputInfo(box, assets)).value.thrushK(trans.xa)

    def getOutputsByAddress(address: Address, paging: Paging): F[Items[OutputInfo]] = {
      val ergoTree = addressToErgoTreeHex(address)
      outputs
        .countAllByErgoTree(ergoTree)
        .flatMap { total =>
          outputs
            .streamAllByErgoTree(ergoTree, paging.offset, paging.limit)
            .chunkN(serviceSettings.chunkSize)
            .through(toOutputInfo)
            .to[List]
            .map(Items(_, total))
        }
        .thrushK(trans.xa)
    }

    /** Get all outputs with the given `address` in proposition & filter outputs spent in the mempool (for unconfirmed transactions)
      */
    private def getUnspentOutputsByAddressD(
      ergoTree: HexString,
      ord: SortOrder,
      excludedBoxes: Option[NonEmptyList[BoxId]]
    ): D[List[OutputInfo]] =
      for {
        unspentOuts <- outputs
                         .streamUnspentByErgoTree(ergoTree, ord.value, excludedBoxes)
                         .chunkN(serviceSettings.chunkSize)
                         .through(toOutputInfo)
                         .to[List]
      } yield unspentOuts

    def `getUnspent&UnconfirmedOutputsMergedByAddress`(
      address: Address,
      ord: SortOrder
    ): F[List[MOutputInfo]] = {
      val ergoTree = addressToErgoTreeNewtype(address)
      (for {
        spentBoxIds <- uinputs.getAllUInputBoxIdsByErgoTree(ergoTree)
        mempoolOutputs <- uoutputs
                            .streamAllRelatedToErgoTree(ergoTree)
                            .chunkN(serviceSettings.chunkSize)
                            .through(memprops.mkUnspentOutputInfo)
                            .to[List]
        unspentBoxes <- getUnspentOutputsByAddressD(ergoTree.value, ord, spentBoxIds.toNel)
      } yield MOutputInfo.fromUOutputList(mempoolOutputs) <+> MOutputInfo.fromOutputList(unspentBoxes)) ||> trans.xa
    }

    def getUnspentOutputsByAddress(address: Address, paging: Paging, ord: SortOrder): F[Items[OutputInfo]] = {
      val ergoTree = addressToErgoTreeHex(address)
      outputs
        .countUnspentByErgoTree(ergoTree)
        .flatMap { total =>
          outputs
            .streamUnspentByErgoTree(ergoTree, paging.offset, paging.limit, ord.value)
            .chunkN(serviceSettings.chunkSize)
            .through(toOutputInfo)
            .to[List]
            .map(Items(_, total))
        }
        .thrushK(trans.xa)
    }

    def getOutputsByErgoTree(ergoTree: HexString, paging: Paging): F[Items[OutputInfo]] =
      outputs
        .countAllByErgoTree(ergoTree)
        .flatMap { total =>
          outputs
            .streamAllByErgoTree(ergoTree, paging.offset, paging.limit)
            .chunkN(serviceSettings.chunkSize)
            .through(toOutputInfo)
            .to[List]
            .map(Items(_, total))
        }
        .thrushK(trans.xa)

    def getUnspentOutputsByErgoTree(ergoTree: HexString, paging: Paging, ord: SortOrder): F[Items[OutputInfo]] =
      outputs
        .countUnspentByErgoTree(ergoTree)
        .flatMap { total =>
          outputs
            .streamUnspentByErgoTree(ergoTree, paging.offset, paging.limit, ord.value)
            .chunkN(serviceSettings.chunkSize)
            .through(toOutputInfo)
            .to[List]
            .map(Items(_, total))
        }
        .thrushK(trans.xa)

    def getOutputsByErgoTreeTemplateHash(hash: ErgoTreeTemplateHash, paging: Paging): F[Items[OutputInfo]] =
      outputs
        .countAllByErgoTreeTemplateHash(hash)
        .flatMap { total =>
          outputs
            .streamAllByErgoTreeTemplateHash(hash, paging.offset, paging.limit)
            .chunkN(serviceSettings.chunkSize)
            .through(toOutputInfo)
            .to[List]
            .map(Items(_, total))
        }
        .thrushK(trans.xa)

    def getUnspentOutputsByErgoTreeTemplateHash(hash: ErgoTreeTemplateHash, paging: Paging): F[Items[OutputInfo]] =
      outputs
        .countUnspentByErgoTreeTemplateHash(hash)
        .flatMap { total =>
          outputs
            .streamUnspentByErgoTreeTemplateHash(hash, paging.offset, paging.limit)
            .chunkN(serviceSettings.chunkSize)
            .through(toUnspentOutputInfo)
            .to[List]
            .map(Items(_, total))
        }
        .thrushK(trans.xa)

    def streamOutputsByErgoTreeTemplateHash(hash: ErgoTreeTemplateHash, range: HeightRange): Stream[F, OutputInfo] =
      outputs
        .streamAllByErgoTreeTemplateHashByEpochs(hash, range.minHeight, range.maxHeight)
        .chunkN(serviceSettings.chunkSize)
        .through(toOutputInfo)
        .thrushK(trans.xas)

    def streamUnspentOutputsByErgoTreeTemplateHash(
      hash: ErgoTreeTemplateHash,
      range: HeightRange
    ): Stream[F, OutputInfo] =
      outputs
        .streamUnspentByErgoTreeTemplateHashByEpochs(hash, range.minHeight, range.maxHeight)
        .chunkN(serviceSettings.chunkSize)
        .through(toUnspentOutputInfo)
        .thrushK(trans.xas)

    def streamUnspentOutputs(range: HeightRange): Stream[F, OutputInfo] =
      outputs
        .streamAllUnspent(range.minHeight, range.maxHeight)
        .chunkN(serviceSettings.chunkSize)
        .through(toUnspentOutputInfo)
        .thrushK(trans.xas)

    def streamUnspentOutputs(suffixLen: Int): Stream[F, OutputInfo] =
      Stream
        .eval(headers.getBestHeight)
        .flatMap { bestHeight =>
          outputs.streamAllUnspent(bestHeight - suffixLen, bestHeight)
        }
        .chunkN(serviceSettings.chunkSize)
        .through(toUnspentOutputInfo)
        .thrushK(trans.xas)

    def streamUnspentOutputs(minGix: Long, limit: Int): Stream[F, OutputInfo] =
      outputs
        .streamAllUnspent(minGix, limit)
        .chunkN(serviceSettings.chunkSize)
        .through(toUnspentOutputInfo)
        .thrushK(trans.xas)

    def streamOutputs(minGix: Long, limit: Int): Stream[F, OutputInfo] =
      outputs
        .streamAll(minGix, limit)
        .chunkN(serviceSettings.chunkSize)
        .through(toUnspentOutputInfo)
        .thrushK(trans.xas)

    def getOutputsByTokenId(tokenId: TokenId, paging: Paging): F[Items[OutputInfo]] =
      outputs
        .countAllByTokenId(tokenId)
        .flatMap { total =>
          outputs
            .getAllByTokenId(tokenId, paging.offset, paging.limit)
            .chunkN(serviceSettings.chunkSize)
            .through(toOutputInfo)
            .to[List]
            .map(Items(_, total))
        }
        .thrushK(trans.xa)

    def getUnspentOutputsByTokenId(tokenId: TokenId, paging: Paging, ord: SortOrder): F[Items[OutputInfo]] =
      outputs
        .countUnspentByTokenId(tokenId)
        .flatMap { total =>
          outputs
            .getUnspentByTokenId(tokenId, paging.offset, paging.limit, ord.value)
            .chunkN(serviceSettings.chunkSize)
            .through(toUnspentOutputInfo)
            .to[List]
            .map(Items(_, total))
        }
        .thrushK(trans.xa)

    def searchAll(boxQuery: BoxQuery, paging: Paging): F[Items[OutputInfo]] = {
      val registers = boxQuery.registers.flatMap(rs => NonEmptyList.fromList(rs.toList))
      val constants = boxQuery.constants.flatMap(cs => NonEmptyList.fromList(cs.toList))
      val assets    = boxQuery.assets.flatMap(NonEmptyList.fromList)
      outputs
        .countAll(boxQuery.ergoTreeTemplateHash, registers, constants, assets)
        .flatMap { total =>
          outputs
            .searchAll(boxQuery.ergoTreeTemplateHash, registers, constants, assets, paging.offset, paging.limit)
            .chunkN(serviceSettings.chunkSize)
            .through(toOutputInfo)
            .to[List]
            .map(Items(_, total))
        }
        .thrushK(trans.xa)
    }

    def searchUnspent(boxQuery: BoxQuery, paging: Paging): F[Items[OutputInfo]] = {
      val registers = boxQuery.registers.flatMap(rs => NonEmptyList.fromList(rs.toList))
      val constants = boxQuery.constants.flatMap(cs => NonEmptyList.fromList(cs.toList))
      val assets    = boxQuery.assets.flatMap(NonEmptyList.fromList)
      outputs
        .countUnspent(boxQuery.ergoTreeTemplateHash, registers, constants, assets)
        .flatMap { total =>
          outputs
            .searchUnspent(boxQuery.ergoTreeTemplateHash, registers, constants, assets, paging.offset, paging.limit)
            .chunkN(serviceSettings.chunkSize)
            .through(toUnspentOutputInfo)
            .to[List]
            .map(Items(_, total))
        }
        .thrushK(trans.xa)
    }

    def searchUnspentByAssetsUnion(boxQuery: BoxAssetsQuery, paging: Paging): F[Items[OutputInfo]] = {
      val assets = boxQuery.assets
      outputs
        .countUnspentByAssetsUnion(boxQuery.ergoTreeTemplateHash, assets)
        .flatMap { total =>
          outputs
            .searchUnspentByAssetsUnion(boxQuery.ergoTreeTemplateHash, assets, paging.offset, paging.limit)
            .chunkN(serviceSettings.chunkSize)
            .through(toUnspentOutputInfo)
            .to[List]
            .map(Items(_, total))
        }
        .thrushK(trans.xa)
    }

    def getAllUnspentOutputs(address: Address, paging: Paging, ord: SortOrder): F[Items[AnyOutputInfo]] = {
      val ergoTree = addressToErgoTreeHex(address)
      (for {
        nUnspent <- uoutputs.countAllByErgoTree(ergoTree)
        boxes <- uoutputs
                   .streamAllUnspentByErgoTree(ergoTree, paging.offset, paging.limit, ord.value)
                   .chunkN(serviceSettings.chunkSize)
                   .through(toAnyOutputInfo)
                   .to[List]
      } yield Items(boxes, nUnspent)).thrushK(trans.xa)
    }

    private def toOutputInfo: Pipe[D, Chunk[ExtendedOutput], OutputInfo] =
      for {
        outs   <- _
        outIds <- Stream.emit(outs.toList.map(_.output.boxId).toNel).unNone
        assets <- assets.getAllByBoxIds(outIds).map(_.groupBy(_.boxId)).asStream
        outsInfo = outs.map(out => OutputInfo(out, assets.getOrElse(out.output.boxId, Nil)))
        flattened <- Stream.emits(outsInfo.toList)
      } yield flattened

    private def toAnyOutputInfo: Pipe[D, Chunk[AnyOutput], AnyOutputInfo] =
      for {
        outs   <- _
        outIds <- Stream.emit(outs.toList.map(_.boxId).toNel).unNone
        assets <- uassets.getConfirmedAndUnconfirmed(outIds).map(_.groupBy(_.boxId)).asStream
        outsInfo = outs.map(out => AnyOutputInfo(out, assets.getOrElse(out.boxId, Nil)))
        flattened <- Stream.emits(outsInfo.toList)
      } yield flattened

    private def toUnspentOutputInfo: Pipe[D, Chunk[Output], OutputInfo] =
      for {
        outs   <- _
        outIds <- Stream.emit(outs.toList.map(_.boxId).toNel).unNone
        assets <- assets.getAllByBoxIds(outIds).map(_.groupBy(_.boxId)).asStream
        outsInfo = outs.map(out => OutputInfo.unspent(out, assets.getOrElse(out.boxId, Nil)))
        flattened <- Stream.emits(outsInfo.toList)
      } yield flattened
  }
}
