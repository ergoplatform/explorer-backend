package org.ergoplatform.explorer.http.api.v1.services

import cats.Monad
import cats.effect.Concurrent
import cats.syntax.list._
import cats.syntax.traverse._
import dev.profunktor.redis4cats.algebra.RedisCommands
import fs2.{Chunk, Pipe, Stream}
import io.estatico.newtype.ops.toCoercibleIdOps
import mouse.anyf._
import org.ergoplatform.explorer.Err.RequestProcessingErr.{BadRequest, FeatureNotSupported}
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.{Output, UOutput, UTransaction}
import org.ergoplatform.explorer.db.repositories.bundles.UtxRepoBundle
import org.ergoplatform.explorer.http.api.models.{HeightRange, Items, Paging}
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v0.models.TxIdResponse
import org.ergoplatform.explorer.http.api.v1.models.{OutputInfo, UOutputInfo, UTransactionInfo}
import org.ergoplatform.explorer.protocol.TxValidation
import org.ergoplatform.explorer.protocol.TxValidation.PartialSemanticValidation
import org.ergoplatform.explorer.protocol.sigma.addressToErgoTreeNewtype
import org.ergoplatform.explorer.settings.{ServiceSettings, UtxCacheSettings}
import org.ergoplatform.explorer.{Address, BoxId, ErgoTree, TxId}
import org.ergoplatform.{ErgoAddressEncoder, ErgoLikeTransaction}
import org.ergoplatform.explorer.syntax.stream._
import tofu.Throws
import tofu.syntax.monadic._
import tofu.syntax.raise._
import tofu.syntax.streams.compile._

trait Mempool[F[_]] {

  def getByErgoTree(
    ergoTree: ErgoTree,
    paging: Paging
  ): F[Items[UTransactionInfo]]

  def getByAddress(
    address: Address,
    paging: Paging
  ): F[Items[UTransactionInfo]]

  def submit(tx: ErgoLikeTransaction): F[TxIdResponse]

  def streamUnspentOutputs: Stream[F, UOutputInfo]
}

object Mempool {

  def apply[F[_]: Concurrent, D[_]: Monad: CompileStream: LiftConnectionIO](
    settings: ServiceSettings,
    utxCacheSettings: UtxCacheSettings,
    redis: Option[RedisCommands[F, String, String]]
  )(trans: D Trans F)(implicit e: ErgoAddressEncoder): F[Mempool[F]] = {
    val validation = PartialSemanticValidation
    UtxRepoBundle[F, D](utxCacheSettings, redis)
      .map(bundle => new Live(settings, bundle, validation)(trans))
  }

  final class Live[F[_]: Monad: Throws, D[_]: Monad: CompileStream](
    settings: ServiceSettings,
    repo: UtxRepoBundle[F, D, Stream],
    semanticValidation: TxValidation
  )(trans: D Trans F)(implicit e: ErgoAddressEncoder)
    extends Mempool[F] {

    import repo._

    def getByErgoTree(ergoTree: ErgoTree, paging: Paging): F[Items[UTransactionInfo]] =
      txs
        .countByErgoTree(ergoTree.value)
        .flatMap { total =>
          txs
            .streamRelatedToErgoTree(ergoTree, paging.offset, paging.limit)
            .chunkN(settings.chunkSize)
            .through(mkTransaction)
            .to[List]
            .map(Items(_, total))
        }
        .thrushK(trans.xa)

    def getByAddress(
      address: Address,
      paging: Paging
    ): F[Items[UTransactionInfo]] =
      getByErgoTree(addressToErgoTreeNewtype(address), paging)

    def submit(tx: ErgoLikeTransaction): F[TxIdResponse] =
      ergoTxRepo match {
        case Some(etx) =>
          val inputErrors = tx.inputs
            .map(i => BoxId.fromErgo(i.boxId))
            .toList
            .zipWithIndex
            .flatTraverse { case (id, idx) =>
              for {
                confirmed   <- confirmedOutputs.getByBoxId(id)
                unconfirmed <- outputs.getByBoxId(id)
                inputNotFound = if (confirmed.isEmpty && unconfirmed.isEmpty) List(s"Input $idx:$id not found")
                                else List.empty
                inputWasSpent = if (confirmed.flatMap(_.spentByOpt).isDefined && confirmed.exists(_.output.mainChain))
                                  List(s"Input $idx:$id was spent")
                                else List.empty
              } yield inputNotFound ++ inputWasSpent
            }
          val semanticErrors = semanticValidation.validate(tx)
          inputErrors.map(_ ++ semanticErrors).thrushK(trans.xa).flatMap {
            case Nil => etx.put(tx) as TxIdResponse(tx.id.toString.coerce[TxId])
            case errors =>
              BadRequest(s"Transaction is invalid. ${errors.mkString("; ")}").raise
          }
        case None => FeatureNotSupported("Tx broadcasting").raise
      }

    def streamUnspentOutputs: Stream[F, UOutputInfo] =
      outputs
        .streamAllUnspent(0, Int.MaxValue)
        .chunkN(settings.chunkSize)
        .through(mkUnspentOutputInfo)
        .thrushK(trans.xas)

    private def mkUnspentOutputInfo: Pipe[D, Chunk[UOutput], UOutputInfo] =
      for {
        outs   <- _
        outIds <- Stream.emit(outs.toList.map(_.boxId).toNel).unNone
        assets <- assets.getAllByBoxIds(outIds).map(_.groupBy(_.boxId)).asStream
        outsInfo = outs.map(out => UOutputInfo.unspent(out, assets.getOrElse(out.boxId, Nil)))
        flattened <- Stream.emits(outsInfo.toList)
      } yield flattened

    private def mkTransaction: Pipe[D, Chunk[UTransaction], UTransactionInfo] =
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
