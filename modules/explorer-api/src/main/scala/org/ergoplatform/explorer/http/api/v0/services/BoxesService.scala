package org.ergoplatform.explorer.http.api.v0.services

import cats.Monad
import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.apply._
import cats.syntax.functor._
import cats.syntax.list._
import fs2.{Chunk, Pipe, Stream}
import mouse.all._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.Err.RefinementFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.AddressDecodingFailed
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer.db.repositories.{AssetRepo, OutputRepo}
import org.ergoplatform.explorer.http.api.models.Sorting
import org.ergoplatform.explorer.http.api.v0.models.OutputInfo
import org.ergoplatform.explorer.protocol.sigma
import org.ergoplatform.explorer.syntax.stream._
import org.ergoplatform.explorer.{Address, BoxId, CRaise, HexString}

trait BoxesService[F[_], S[_[_], _]] {

  /** Get output by `boxId`.
    */
  def getOutputById(id: BoxId): F[Option[OutputInfo]]

  /** Get all outputs with the given `address` in proposition.
    */
  def getOutputsByAddress(address: Address): S[F, OutputInfo]

  /** Get unspent outputs with the given `address` in proposition.
    */
  def getUnspentOutputsByAddress(address: Address): S[F, OutputInfo]

  /** Get all outputs with the given `ergoTree` in proposition.
    */
  def getOutputsByErgoTree(ergoTree: HexString): S[F, OutputInfo]

  /** Get unspent outputs with the given `ergoTree` in proposition.
    */
  def getUnspentOutputsByErgoTree(ergoTree: HexString): S[F, OutputInfo]
}

object BoxesService {

  def apply[
    F[_]: Sync,
    D[_]: CRaise[*[_], AddressDecodingFailed]: CRaise[*[_], RefinementFailed]: LiftConnectionIO: Monad
  ](trans: D Trans F)(implicit e: ErgoAddressEncoder): F[BoxesService[F, Stream]] =
    (OutputRepo[F, D], AssetRepo[F, D]).mapN(new Live(_, _)(trans))

  final private class Live[
    F[_],
    D[_]: CRaise[*[_], AddressDecodingFailed]: CRaise[*[_], RefinementFailed]: Monad
  ](
    outRepo: OutputRepo[D, Stream],
    assetRepo: AssetRepo[D, Stream]
  )(trans: D Trans F)(implicit e: ErgoAddressEncoder)
    extends BoxesService[F, Stream] {

    def getOutputById(id: BoxId): F[Option[OutputInfo]] =
      (for {
        box    <- OptionT(outRepo.getByBoxId(id))
        assets <- OptionT.liftF(assetRepo.getAllByBoxId(box.output.boxId))
      } yield OutputInfo(box, assets)).value ||> trans.xa

    def getOutputsByAddress(address: Address): Stream[F, OutputInfo] =
      (sigma.addressToErgoTreeHex(address) |> (outRepo
        .streamAllByErgoTree(_, 0, Int.MaxValue)
        .chunkN(100))).through(toOutputInfo) ||> trans.xas

    def getUnspentOutputsByAddress(address: Address): Stream[F, OutputInfo] =
      (sigma.addressToErgoTreeHex(address) |> (outRepo
        .streamUnspentByErgoTree(_, 0, Int.MaxValue, Sorting.Asc.value)
        .chunkN(100))).through(toOutputInfo) ||> trans.xas

    def getOutputsByErgoTree(ergoTree: HexString): Stream[F, OutputInfo] =
      outRepo
        .streamAllByErgoTree(ergoTree, 0, Int.MaxValue)
        .chunkN(100)
        .through(toOutputInfo) ||> trans.xas

    def getUnspentOutputsByErgoTree(ergoTree: HexString): Stream[F, OutputInfo] =
      outRepo
        .streamUnspentByErgoTree(ergoTree, 0, Int.MaxValue, Sorting.Asc.value)
        .chunkN(100)
        .through(toOutputInfo) ||> trans.xas

    private def toOutputInfo: Pipe[D, Chunk[ExtendedOutput], OutputInfo] =
      for {
        outs   <- _
        outIds <- Stream.emit(outs.toList.map(_.output.boxId).toNel).unNone
        assets <- assetRepo.getAllByBoxIds(outIds).map(_.groupBy(_.boxId)).asStream
        outsInfo = outs.map(out => OutputInfo(out, assets.getOrElse(out.output.boxId, Nil)))
        flattened <- Stream.emits(outsInfo.toList)
      } yield flattened
  }
}
