package org.ergoplatform.explorer.http.api.v1.services

import cats.Monad
import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.apply._
import cats.syntax.list._
import mouse.anyf._
import fs2.{Chunk, Pipe, Stream}
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.Err.{RefinementFailed, RequestProcessingErr}
import org.ergoplatform.explorer.Err.RequestProcessingErr.AddressDecodingFailed
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.Output
import org.ergoplatform.explorer.db.repositories.{AssetRepo, HeaderRepo, OutputRepo}
import org.ergoplatform.explorer.{Address, BoxId, CRaise, HexString}
import org.ergoplatform.explorer.http.api.v0.models.OutputInfo
import org.ergoplatform.explorer.http.api.v1.UnspentOutputInfo
import org.ergoplatform.explorer.protocol.utils
import org.ergoplatform.explorer.settings.ServiceSettings
import org.ergoplatform.explorer.syntax.stream._
import tofu.syntax.raise._

trait BoxesService[F[_]] {

  /** Get all unspent outputs appeared in the blockchain after `minHeight`.
    */
  def getUnspentOutputs(minHeight: Int, maxHeight: Int): Stream[F, UnspentOutputInfo]

  /** Get all unspent outputs appeared in the blockchain after `minHeight`.
    */
  def getUnspentOutputs(lastEpochs: Int): Stream[F, UnspentOutputInfo]
}

object BoxesService {

  def apply[
    F[_]: Sync,
    D[_]: CRaise[*[_], RequestProcessingErr]: CRaise[*[_], RefinementFailed]: LiftConnectionIO: Monad
  ](trans: D Trans F)(implicit e: ErgoAddressEncoder): F[BoxesService[F]] =
    (HeaderRepo[F, D], OutputRepo[F, D], AssetRepo[F, D]).mapN(new Live(_, _, _)(trans))

  final private class Live[
    F[_],
    D[_]: CRaise[*[_], RequestProcessingErr]: CRaise[*[_], RefinementFailed]: Monad
  ](
    headerRepo: HeaderRepo[D],
    outRepo: OutputRepo[D, Stream],
    assetRepo: AssetRepo[D, Stream]
  )(trans: D Trans F)(implicit e: ErgoAddressEncoder)
    extends BoxesService[F] {

    def getUnspentOutputs(minHeight: Int, maxHeight: Int): Stream[F, UnspentOutputInfo] =
      outRepo
        .getAllMainUnspent(minHeight, maxHeight)
        .chunkN(100)
        .through(toOutputInfo)
        .thrushK(trans.xas)

    def getUnspentOutputs(lastEpochs: Int): Stream[F, UnspentOutputInfo] =
      Stream
        .eval(headerRepo.getBestHeight)
        .flatMap { bestHeight =>
          outRepo.getAllMainUnspent(bestHeight - lastEpochs, bestHeight)
        }
        .chunkN(100)
        .through(toOutputInfo)
        .thrushK(trans.xas)

    private def toOutputInfo: Pipe[D, Chunk[Output], UnspentOutputInfo] =
      for {
        outs   <- _
        outIds <- Stream.emit(outs.toList.map(_.boxId).toNel).unNone
        assets <- assetRepo.getAllByBoxIds(outIds).map(_.groupBy(_.boxId)).asStream
        outsInfo = outs.map(out => UnspentOutputInfo(out, assets.getOrElse(out.boxId, Nil)))
        flattened <- Stream.emits(outsInfo.toList)
      } yield flattened
  }
}
