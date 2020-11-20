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
import org.ergoplatform.explorer.Err.RefinementFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.AddressDecodingFailed
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer.db.repositories.{AssetRepo, OutputRepo}
import org.ergoplatform.explorer.{Address, BoxId, CRaise, HexString}
import org.ergoplatform.explorer.http.api.v0.models.OutputInfo
import org.ergoplatform.explorer.protocol.utils
import org.ergoplatform.explorer.syntax.stream._

trait BoxesService[F[_], S[_[_], _]] {

  /** Get all unspent outputs appeared in the blockchain after `minHeight`.
    */
  def getUnspentOutputs(minHeight: Int): S[F, OutputInfo]
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

    def getUnspentOutputs(minHeight: Int): Stream[F, OutputInfo] = ???

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
