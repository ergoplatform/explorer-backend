package org.ergoplatform.explorer.http.api.v0.services

import cats.instances.option._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.list._
import cats.syntax.traverse._
import cats.{~>, Monad}
import fs2.Stream
import mouse.anyf._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.algebra.Raise
import org.ergoplatform.explorer.db.models.UTransaction
import org.ergoplatform.explorer.db.repositories.{
  UAssetRepo,
  UInputRepo,
  UOutputRepo,
  UTransactionRepo
}
import org.ergoplatform.explorer.http.api.v0.models.{
  UInputInfo,
  UOutputInfo,
  UTransactionInfo
}
import org.ergoplatform.explorer.protocol.utils
import org.ergoplatform.explorer.{Address, Err, HexString, TxId}
import org.ergoplatform.explorer.syntax.streamEffect._
import org.ergoplatform.explorer.syntax.option._
import scorex.util.encode.Base16

/** A service providing an access to unconfirmed transactions data.
  */
trait OffChainService[F[_], S[_[_], _]] {

  /** Get unconfirmed transaction with a given `id`.
    */
  def getUnconfirmedTxById(id: TxId): F[Option[UTransactionInfo]]

  /** Get all unconfirmed transactions related to the given `address`.
    */
  def getUnconfirmedTxsByAddress(address: Address): S[F, UTransactionInfo]

  /** Get all unconfirmed transactions related to the given `ergoTree`.
    */
  def getUnconfirmedTxsByErgoTree(ergoTree: HexString): S[F, UTransactionInfo]

  /** Get all inputs containing in unconfirmed transactions.
    */
  def getAllUnconfirmedInputs: S[F, UInputInfo]

  /** Get all outputs containing in unconfirmed transactions.
    */
  def getAllUnconfirmedOutputs: S[F, UOutputInfo]
}

object OffChainService {

  final private class Live[F[_], D[_]: Raise[*[_], Err]: Monad](
    txRepo: UTransactionRepo[D, Stream],
    inRepo: UInputRepo[D, Stream],
    outRepo: UOutputRepo[D, Stream],
    assetRepo: UAssetRepo[D]
  )(xa: D ~> F)(implicit e: ErgoAddressEncoder)
    extends OffChainService[F, Stream] {

    def getUnconfirmedTxById(id: TxId): F[Option[UTransactionInfo]] =
      txRepo.get(id).flatMap(_.traverse(completeTransaction)) ||> xa

    def getUnconfirmedTxsByAddress(address: Address): Stream[F, UTransactionInfo] =
      utils
        .addressToErgoTree[D](address)
        .flatMap(tree => HexString.fromString(Base16.encode(tree.bytes)))
        .asStream
        .flatMap(txRepo.getAllRelatedToErgoTree(_, offset = 0, limit = Int.MaxValue))
        .flatMap(completeTransaction(_).asStream)
        .translate(xa)

    def getUnconfirmedTxsByErgoTree(ergoTree: HexString): Stream[F, UTransactionInfo] =
      txRepo
        .getAllRelatedToErgoTree(ergoTree, offset = 0, limit = Int.MaxValue)
        .flatMap(completeTransaction(_).asStream)
        .translate(xa)

    def getAllUnconfirmedInputs: Stream[F, UInputInfo] =
      inRepo.getAll(offset = 0, limit = Int.MaxValue).map(UInputInfo.apply).translate(xa)

    def getAllUnconfirmedOutputs: Stream[F, UOutputInfo] =
      (for {
        out    <- outRepo.getAll(offset = 0, limit = Int.MaxValue)
        assets <- assetRepo.getAllByBoxId(out.boxId).asStream
      } yield UOutputInfo(out, assets)).translate(xa)

    private def completeTransaction(tx: UTransaction): D[UTransactionInfo] =
      for {
        ins       <- inRepo.getAllByTxId(tx.id)
        outs      <- outRepo.getAllByTxId(tx.id)
        boxIdsNel <- outs.map(_.boxId).toNel.liftTo[D](Err.InconsistentDbData("Empty outputs"))
        assets    <- assetRepo.getAllByBoxIds(boxIdsNel)
      } yield UTransactionInfo(tx, ins, outs, assets)
  }
}
