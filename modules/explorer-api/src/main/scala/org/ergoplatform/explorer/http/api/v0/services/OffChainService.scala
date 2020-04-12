package org.ergoplatform.explorer.http.api.v0.services

import cats.effect.Sync
import cats.instances.option._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.list._
import cats.syntax.apply._
import cats.syntax.traverse._
import cats.{Monad, ~>}
import fs2.Stream
import mouse.anyf._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.Err.{RefinementFailed, RequestProcessingErr}
import org.ergoplatform.explorer.Err.RequestProcessingErr.InconsistentDbData
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.UTransaction
import org.ergoplatform.explorer.db.repositories.{UAssetRepo, UInputRepo, UOutputRepo, UTransactionRepo}
import org.ergoplatform.explorer.http.api.models.Paging
import org.ergoplatform.explorer.http.api.v0.models.{UInputInfo, UOutputInfo, UTransactionInfo}
import org.ergoplatform.explorer.protocol.utils
import org.ergoplatform.explorer.{Address, CRaise, Err, HexString, TxId}
import org.ergoplatform.explorer.syntax.stream._
import tofu.syntax.raise._

/** A service providing an access to unconfirmed transactions data.
  */
trait OffChainService[F[_], S[_[_], _]] {

  /** Get unconfirmed transaction with a given `id`.
    */
  def getUnconfirmedTxById(id: TxId): F[Option[UTransactionInfo]]

  /** Get all unconfirmed transactions related to the given `address`.
    */
  def getUnconfirmedTxsByAddress(address: Address, paging: Paging): S[F, UTransactionInfo]

  /** Get all unconfirmed transactions related to the given `ergoTree`.
    */
  def getUnconfirmedTxsByErgoTree(
    ergoTree: HexString,
    paging: Paging
  ): S[F, UTransactionInfo]

  /** Get all inputs containing in unconfirmed transactions.
    */
  def getAllUnconfirmedInputs: S[F, UInputInfo]

  /** Get all outputs containing in unconfirmed transactions.
    */
  def getAllUnconfirmedOutputs: S[F, UOutputInfo]
}

object OffChainService {

  def apply[F[_]: Sync, D[_]: CRaise[*[_], Err]: Monad: LiftConnectionIO](
    xa: D ~> F
  )(implicit e: ErgoAddressEncoder): F[OffChainService[F, Stream]] =
    (UTransactionRepo[F, D], UInputRepo[F, D], UOutputRepo[F, D], UAssetRepo[F, D])
      .mapN(new Live(_, _, _, _)(xa))

  final private class Live[
    F[_],
    D[_]: CRaise[*[_], RequestProcessingErr]: CRaise[*[_], RefinementFailed]: Monad
  ](
    txRepo: UTransactionRepo[D, Stream],
    inRepo: UInputRepo[D, Stream],
    outRepo: UOutputRepo[D, Stream],
    assetRepo: UAssetRepo[D]
  )(xa: D ~> F)(implicit e: ErgoAddressEncoder)
    extends OffChainService[F, Stream] {

    def getUnconfirmedTxById(id: TxId): F[Option[UTransactionInfo]] =
      txRepo.get(id).flatMap(_.traverse(completeTransaction)) ||> xa

    def getUnconfirmedTxsByAddress(
      address: Address,
      paging: Paging
    ): Stream[F, UTransactionInfo] =
      utils
        .addressToErgoTreeHex(address)
        .asStream
        .flatMap(getUtxInfoBatched(_, paging))
        .translate(xa)

    def getUnconfirmedTxsByErgoTree(
      ergoTree: HexString,
      paging: Paging
    ): Stream[F, UTransactionInfo] =
      getUtxInfoBatched(ergoTree, paging).translate(xa)

    def getAllUnconfirmedInputs: Stream[F, UInputInfo] =
      inRepo.getAll(offset = 0, limit = Int.MaxValue).map(UInputInfo.apply).translate(xa)

    def getAllUnconfirmedOutputs: Stream[F, UOutputInfo] =
      (for {
        out    <- outRepo.getAll(offset = 0, limit = Int.MaxValue)
        assets <- assetRepo.getAllByBoxId(out.boxId).asStream
      } yield UOutputInfo(out, assets)).translate(xa)

    private def completeTransaction(tx: UTransaction): D[UTransactionInfo] =
      for {
        ins  <- inRepo.getAllByTxId(tx.id)
        outs <- outRepo.getAllByTxId(tx.id)
        boxIdsNel <- outs
                      .map(_.boxId)
                      .toNel
                      .orRaise[D](InconsistentDbData("Empty outputs"))
        assets <- assetRepo.getAllByBoxIds(boxIdsNel)
      } yield UTransactionInfo(tx, ins, outs, assets)

    private def getUtxInfoBatched(
      ergoTree: HexString,
      paging: Paging
    ): Stream[D, UTransactionInfo] =
      for {
        txChunk <- txRepo
                    .getAllRelatedToErgoTree(ergoTree, paging.offset, paging.limit)
                    .chunkN(100)
        txIdsNel  <- txChunk.map(_.id).toList.toNel.toStream.covary[D]
        ins       <- inRepo.getAllByTxIds(txIdsNel).asStream
        outs      <- outRepo.getAllByTxIds(txIdsNel).asStream
        boxIdsNel <- outs.map(_.boxId).toNel.toStream.covary[D]
        assets    <- assetRepo.getAllByBoxIds(boxIdsNel).asStream
        utxInfo   <- Stream.emits(UTransactionInfo.batch(txChunk.toList, ins, outs, assets))
      } yield utxInfo
  }
}
