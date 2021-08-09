package org.ergoplatform.explorer.http.api.v1.services

import cats.Monad
import cats.syntax.list._
import fs2.{Chunk, Pipe, Stream}
import mouse.anyf._
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.models.UTransaction
import org.ergoplatform.explorer.db.repositories.bundles.UtxRepoBundle
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v0.models.TxIdResponse
import org.ergoplatform.explorer.http.api.v1.models.UTransactionInfo
import org.ergoplatform.explorer.protocol.sigma.addressToErgoTreeNewtype
import org.ergoplatform.explorer.settings.ServiceSettings
import org.ergoplatform.explorer.{Address, ErgoTree}
import org.ergoplatform.{ErgoAddressEncoder, ErgoLikeTransaction}
import tofu.syntax.monadic._
import tofu.syntax.streams.compile._

trait UnconfirmedTransactions[F[_]] {

  def getByErgoTree(
    ergoTree: ErgoTree,
    paging: Paging
  ): F[Items[UTransactionInfo]]

  def getByAddress(
    address: Address,
    paging: Paging
  ): F[Items[UTransactionInfo]]

  def submit(tx: ErgoLikeTransaction): F[TxIdResponse]
}

object UnconfirmedTransactions {

  final class Live[F[_]: Monad, D[_]: Monad: CompileStream](
    settings: ServiceSettings,
    repo: UtxRepoBundle[D, Stream]
  )(trans: D Trans F)(implicit e: ErgoAddressEncoder)
    extends UnconfirmedTransactions[F] {

    import repo._

    def getByErgoTree(ergoTree: ErgoTree, paging: Paging): F[Items[UTransactionInfo]] =
      txs
        .countByErgoTree(ergoTree.value)
        .flatMap { total =>
          txs
            .streamRelatedToErgoTree(ergoTree, paging.offset, paging.limit)
            .chunkN(settings.chunkSize)
            .through(makeTransaction)
            .to[List]
            .map(Items(_, total))
        }
        .thrushK(trans.xa)

    def getByAddress(
      address: Address,
      paging: Paging
    ): F[Items[UTransactionInfo]] =
      getByErgoTree(addressToErgoTreeNewtype(address), paging)

    def submit(tx: ErgoLikeTransaction): F[TxIdResponse] = ???

    private def makeTransaction: Pipe[D, Chunk[UTransaction], UTransactionInfo] =
      for {
        chunk     <- _
        txIds     <- Stream.emit(chunk.map(_.id).toNel).unNone
        ins       <- Stream.eval(inputs.getAllByTxIds(txIds))
        inIds     <- Stream.emit(ins.map(_.input.boxId).toNel).unNone
        inAssets  <- Stream.eval(assets.getAllByBoxIds(inIds))
        dataIns   <- Stream.eval(dataInputs.getAllByTxIds(txIds))
        outs      <- Stream.eval(outputs.getAllByTxIds(txIds))
        outIds    <- Stream.emit(outs.map(_.boxId).toNel).unNone
        outAssets <- Stream.eval(assets.getAllByBoxIds(outIds))
        txInfo <-
          Stream.emits(UTransactionInfo.unFlattenBatch(chunk.toList, ins, dataIns, outs, inAssets, outAssets))
      } yield txInfo
  }
}
