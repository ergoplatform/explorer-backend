package org.ergoplatform.explorer.http.api.v1.shared

import cats.Monad
import cats.effect.Concurrent
import cats.syntax.list._
import dev.profunktor.redis4cats.algebra.RedisCommands
import fs2.{Chunk, Pipe, Stream}
import mouse.anyf._
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, ExtendedUAsset, ExtendedUInput, ExtendedUOutput}
import org.ergoplatform.explorer.db.models.{UOutput, UTransaction}
import org.ergoplatform.explorer.db.repositories.bundles.UtxRepoBundle
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v1.models.{UInputInfo, UOutputInfo, UTransactionInfo}
import org.ergoplatform.explorer.settings.{ServiceSettings, UtxCacheSettings}
import org.ergoplatform.explorer.{Address, BoxId, ErgoTree, TxId}
import org.ergoplatform.{explorer, ErgoAddressEncoder}
import org.ergoplatform.explorer.protocol.sigma.addressToErgoTreeNewtype
import org.ergoplatform.explorer.syntax.stream._
import tofu.Throws
import tofu.syntax.monadic._
import tofu.syntax.streams.compile._
import tofu.syntax.raise._

trait MempoolProps[F[_], D[_]] {
  def hasUnconfirmedBalance(ergoTree: ErgoTree): F[Boolean]
  def mkUnspentOutputInfo: Pipe[D, Chunk[UOutput], UOutputInfo]
  def mkTransaction: Pipe[D, Chunk[UTransaction], UTransactionInfo]
}

object MempoolProps {

  def apply[F[_]: Concurrent, D[_]: Monad: CompileStream: LiftConnectionIO](
    settings: ServiceSettings,
    utxCacheSettings: UtxCacheSettings,
    redis: Option[RedisCommands[F, String, String]]
  )(trans: D Trans F)(implicit e: ErgoAddressEncoder): F[MempoolProps[F, D]] =
    UtxRepoBundle[F, D](utxCacheSettings, redis)
      .map(bundle => new Live(settings, bundle)(trans))

  final class Live[F[_]: Monad: Throws, D[_]: Monad: CompileStream](
    settings: ServiceSettings,
    repo: UtxRepoBundle[F, D, Stream]
  )(trans: D Trans F)(implicit e: ErgoAddressEncoder)
    extends MempoolProps[F, D] {

    import repo._

    def hasUnconfirmedBalance(ergoTree: ErgoTree): F[Boolean] =
      txs
        .countByErgoTree(ergoTree.value)
        .map(_ > 0)
        .thrushK(trans.xa)

    def mkUnspentOutputInfo: Pipe[D, Chunk[UOutput], UOutputInfo] =
      for {
        outs   <- _
        outIds <- Stream.emit(outs.toList.map(_.boxId).toNel).unNone
        assets <- assets.getAllByBoxIds(outIds).map(_.groupBy(_.boxId)).asStream
        outsInfo = outs.map(out => UOutputInfo.unspent(out, assets.getOrElse(out.boxId, Nil)))
        flattened <- Stream.emits(outsInfo.toList)
      } yield flattened

    def mkTransaction: Pipe[D, Chunk[UTransaction], UTransactionInfo] =
      for {
        chunk        <- _
        txIds        <- Stream.emit(chunk.map(_.id).toNel).unNone
        ins          <- Stream.eval(inputs.getAllByTxIds(txIds))
        inIds        <- Stream.emit(ins.map(_.input.boxId).toNel).unNone
        inAssets     <- Stream.eval(assets.getAllByBoxIds(inIds))
        confInAssets <- Stream.eval(confirmedAssets.getAllByBoxIds(inIds))
        dataIns      <- Stream.eval(dataInputs.getAllByTxIds(txIds))
        outs         <- Stream.eval(outputs.getAllByTxIds(txIds))
        outIds       <- Stream.emit(outs.map(_.output.boxId).toNel).unNone
        outAssets    <- Stream.eval(assets.getAllByBoxIds(outIds))
        txInfo <-
          Stream.emits(
            UTransactionInfo.unFlattenBatch(chunk.toList, ins, dataIns, outs, inAssets, confInAssets, outAssets)
          )
      } yield txInfo
  }
}
