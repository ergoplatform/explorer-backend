package org.ergoplatform.explorer.http.api.v1.services

import cats.{Monad, Parallel}
import cats.effect.Sync
import fs2.{Chunk, Pipe, Stream}
import mouse.anyf._
import cats.syntax.parallel._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.{Address, HexString}
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v1.models.{AddressInfo, Balance, TokenAmount, TotalBalance}
import org.ergoplatform.explorer.http.api.v1.shared.MempoolProps
import org.ergoplatform.explorer.protocol.sigma
import org.ergoplatform.explorer.settings.ServiceSettings
import tofu.syntax.monadic._
import tofu.syntax.raise._
import tofu.syntax.streams.compile._

trait Addresses[F[_]] {

  def confirmedBalanceOf(address: Address, minConfirmations: Int): F[Balance]

  def totalBalanceOf(address: Address): F[TotalBalance]

  def addressInfoOf(batch: List[Address], minConfirmations: Int = 0): F[Map[Address, AddressInfo]]
}

object Addresses {

  def apply[F[_]: Sync: Parallel, D[_]: Monad: CompileStream: LiftConnectionIO](
    settings: ServiceSettings,
    memprops: MempoolProps[F, D]
  )(trans: D Trans F)(implicit
    e: ErgoAddressEncoder
  ): F[Addresses[F]] =
    (HeaderRepo[F, D], OutputRepo[F, D], AssetRepo[F, D], UOutputRepo[F, D], UAssetRepo[F, D], UTransactionRepo[F, D])
      .mapN(new Live(settings, memprops, _, _, _, _, _, _)(trans))

  final class Live[F[_]: Monad: Parallel, D[_]: Monad: CompileStream](
    settings: ServiceSettings,
    memprops: MempoolProps[F, D],
    headerRepo: HeaderRepo[D, Stream],
    outputRepo: OutputRepo[D, Stream],
    assetRepo: AssetRepo[D, Stream],
    uOutputRepo: UOutputRepo[D, Stream],
    uAssetRepo: UAssetRepo[D],
    uTransactionRepo: UTransactionRepo[D, Stream]
  )(trans: D Trans F)(implicit e: ErgoAddressEncoder)
    extends Addresses[F] {

    def confirmedBalanceOf(address: Address, minConfirmations: Int): F[Balance] =
      (for {
        height <- if (minConfirmations > 0) headerRepo.getBestHeight else Int.MaxValue.pure[D]
        maxHeight = height - minConfirmations
        tree      = sigma.addressToErgoTreeHex(address)
        balance <- outputRepo.sumUnspentByErgoTree(tree, maxHeight)
        assets  <- assetRepo.aggregateUnspentByErgoTree(tree, maxHeight)
      } yield Balance(balance, assets.map(TokenAmount(_)))) ||> trans.xa

    def totalBalanceOf(address: Address): F[TotalBalance] = {
      val tree = sigma.addressToErgoTreeHex(address)
      (for {
        balance         <- outputRepo.sumUnspentByErgoTree(tree, Int.MaxValue)
        assets          <- assetRepo.aggregateUnspentByErgoTree(tree, Int.MaxValue)
        offChainBalance <- uOutputRepo.sumUnspentByErgoTree(tree)
        offChainAssets  <- uAssetRepo.aggregateUnspentByErgoTree(tree)
      } yield TotalBalance(
        confirmed   = Balance(balance, assets.map(TokenAmount(_))),
        unconfirmed = Balance(offChainBalance, offChainAssets.map(TokenAmount(_)))
      )) ||> trans.xa
    }

    private def hasBeenUsedByErgoTree(ergoTree: HexString): F[Boolean] =
      outputRepo.countAllByErgoTree(ergoTree).map(_ > 0) ||> trans.xa

    def addressInfoOf(batch: List[Address], minConfirmations: Int = 0): F[Map[Address, AddressInfo]] =
      (for {
        height <- if (minConfirmations > 0) headerRepo.getBestHeight else Int.MaxValue.pure[D]
        maxHeight = height - minConfirmations
      } yield maxHeight).flatMap { mH =>
        Stream
          .emits[D, Address](batch)
          .chunkN(settings.chunkSize)
          .through(mkBatch(maxHeight = mH))
          .to[List]
          .map(_.toMap)
      } ||> trans.xa

    private def mkBatch(maxHeight: Int): Pipe[D, Chunk[Address], (Address, AddressInfo)] =
      for {
        chunk <- _
        chunkL = chunk.map(c => (c, sigma.addressToErgoTreeHex(c))).toList
        chunkHex         <- Stream.emit(chunk.map(sigma.addressToErgoTreeHex(_)).toNel).unNone
        batchUnspentSums <- Stream.eval(outputRepo.sumUnspentByErgoTree(chunkHex, maxHeight))
        batchAssets      <- Stream.eval(assetRepo.aggregateUnspentByErgoTree(chunkHex, maxHeight))
        batchUsedState   <- Stream.eval(outputRepo.getUsedStateByErgoTree(chunkHex))
        batchUTxState    <- Stream.eval(uTransactionRepo.getUnconfirmedTransactionsState(chunkHex))
        batchInfo <- Stream.emits(
                       AddressInfo.makeInfo(
                         chunkL,
                         batchUnspentSums,
                         batchAssets,
                         batchUsedState,
                         batchUTxState
                       )
                     )
      } yield batchInfo
  }
}
