package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import doobie.free.implicits._
import doobie.refined.implicits._
import doobie.util.log.LogHandler
import fs2.Stream
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.models.Output
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer.{Address, BoxId, HexString, Id, TokenId, TxId}
import org.ergoplatform.explorer.db.doobieInstances._

/** [[Output]] and [[ExtendedOutput]] data access operations.
  */
trait OutputRepo[D[_], S[_[_], _]] {

  /** Put a given `output` to persistence.
    */
  def insert(output: Output): D[Unit]

  /** Put a given list of outputs to persistence.
    */
  def insertMany(outputs: List[Output]): D[Unit]

  /** Get an output with a given `boxId` from persistence.
    */
  def getByBoxId(boxId: BoxId): D[Option[ExtendedOutput]]

  /** Get all outputs with a given `ergoTree` appeared in the blockchain before `maxHeight`.
    */
  def getAllMainByErgoTree(ergoTree: HexString, maxHeight: Int): D[List[ExtendedOutput]]

  /** Get outputs with a given `ergoTree` from persistence.
    */
  def getMainByErgoTree(ergoTree: HexString, offset: Int, limit: Int): S[D, ExtendedOutput]

  /** Get all unspent main-chain outputs with a given `ergoTree` from persistence.
    */
  def getAllMainUnspentIdsByErgoTree(ergoTree: HexString): D[List[BoxId]]

  /** Get total amount of all unspent main-chain outputs with a given `ergoTree`.
    */
  def sumOfAllMainUnspentByErgoTree(ergoTree: HexString, minConfirmations: Int): D[Long]

  /** Get balances of all addresses in the network.
    */
  def balanceStatsMain(offset: Int, limit: Int): D[List[(Address, Long)]]

  /** Get total number of addresses in the network.
    */
  def totalAddressesMain: D[Int]

  /** Get unspent main-chain outputs with a given `ergoTree` from persistence.
    */
  def getMainUnspentByErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int
  ): S[D, ExtendedOutput]

  /** Get all unspent main-chain outputs that are protected with given ergo tree template
    * see [[https://github.com/ScorexFoundation/sigmastate-interpreter/issues/264]]
    * [[http://github.com/ScorexFoundation/sigmastate-interpreter/blob/633efcfd47f2fa4aa240eee2f774cc033cc241a5/sigmastate/src/main/scala/sigmastate/Values.scala#L828-L828]]
    */
  def getAllMainUnspentByErgoTreeTemplate(
    ergoTreeTemplate: HexString,
    offset: Int,
    limit: Int
  ): S[D, ExtendedOutput]

  /** Get all outputs related to a given `txId`.
    */
  def getAllByTxId(txId: TxId): D[List[ExtendedOutput]]

  /** Get all outputs related to a given list of `txId`.
    */
  def getAllByTxIds(txsId: NonEmptyList[TxId]): D[List[ExtendedOutput]]

  /** Get all unspent main-chain DEX sell orders
    */
  def getAllMainUnspentSellOrderByTokenId(
    tokenId: TokenId,
    ergoTreeTemplate: HexString,
    offset: Int,
    limit: Int
  ): S[D, ExtendedOutput]

  /** Get all unspent main-chain DEX buy orders
    */
  def getAllMainUnspentBuyOrderByTokenId(
    tokenId: TokenId,
    ergoTreeTemplate: HexString,
    offset: Int,
    limit: Int
  ): S[D, ExtendedOutput]

  /** Get all addresses matching the given `query`.
    */
  def getAllLike(query: String): D[List[Address]]

  def sumOfAllUnspentOutputsSince(ts: Long): D[BigDecimal]

  def estimatedOutputsSince(ts: Long)(genesisAddress: Address): D[BigDecimal]

  /** Update main_chain status for all outputs related to given `headerId`.
    */
  def updateChainStatusByHeaderId(headerId: Id, newChainStatus: Boolean): D[Unit]

  /** Get all unspent outputs appeared in the main chain after `minHeight`.
    */
  def getAllMainUnspent(minHeight: Int, maxHeight: Int): S[D, Output]

  def getAllByTokenId(tokenId: TokenId, offset: Int, limit: Int): S[D, ExtendedOutput]

  def getUnspentByTokenId(tokenId: TokenId, offset: Int, limit: Int): S[D, Output]
}

object OutputRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[OutputRepo[D, Stream]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler) extends OutputRepo[D, Stream] {

    import org.ergoplatform.explorer.db.queries.{OutputQuerySet => QS}

    private val liftK = LiftConnectionIO[D].liftConnectionIOK

    def insert(output: Output): D[Unit] =
      QS.insertNoConflict(output).void.liftConnectionIO

    def insertMany(outputs: List[Output]): D[Unit] =
      QS.insertManyNoConflict(outputs).void.liftConnectionIO

    def getByBoxId(boxId: BoxId): D[Option[ExtendedOutput]] =
      QS.getByBoxId(boxId).option.liftConnectionIO

    def getAllMainByErgoTree(ergoTree: HexString, maxHeight: Int): D[List[ExtendedOutput]] =
      QS.getMainByErgoTree(ergoTree, offset = 0, limit = Int.MaxValue, maxHeight = maxHeight)
        .to[List]
        .liftConnectionIO

    def getMainByErgoTree(
      ergoTree: HexString,
      offset: Int,
      limit: Int
    ): Stream[D, ExtendedOutput] =
      QS.getMainByErgoTree(ergoTree, offset, limit).stream.translate(liftK)

    def getAllMainUnspentIdsByErgoTree(ergoTree: HexString): D[List[BoxId]] =
      QS.getAllMainUnspentIdsByErgoTree(ergoTree)
        .to[List]
        .liftConnectionIO

    def sumOfAllMainUnspentByErgoTree(ergoTree: HexString, maxHeight: Int): D[Long] =
      QS.sumOfAllMainUnspentByErgoTree(ergoTree, maxHeight).unique.liftConnectionIO

    def balanceStatsMain(offset: Int, limit: Int): D[List[(Address, Long)]] =
      QS.balanceStatsMain(offset, limit).to[List].liftConnectionIO

    def totalAddressesMain: D[Int] =
      QS.totalAddressesMain.unique.liftConnectionIO

    def getMainUnspentByErgoTree(
      ergoTree: HexString,
      offset: Int,
      limit: Int
    ): Stream[D, ExtendedOutput] =
      QS.getMainUnspentByErgoTree(ergoTree, offset, limit).stream.translate(liftK)

    def getAllMainUnspentByErgoTreeTemplate(
      ergoTreeTemplate: HexString,
      offset: Int,
      limit: Int
    ): Stream[D, ExtendedOutput] =
      QS.getMainUnspentByErgoTreeTemplate(ergoTreeTemplate, offset, limit)
        .stream
        .translate(liftK)

    def getAllByTxId(txId: TxId): D[List[ExtendedOutput]] =
      QS.getAllByTxId(txId).to[List].liftConnectionIO

    def getAllByTxIds(txIds: NonEmptyList[TxId]): D[List[ExtendedOutput]] =
      QS.getAllByTxIds(txIds).to[List].liftConnectionIO

    def getAllMainUnspentSellOrderByTokenId(
      tokenId: TokenId,
      ergoTreeTemplate: HexString,
      offset: Int,
      limit: Int
    ): Stream[D, ExtendedOutput] =
      QS.getMainUnspentSellOrderByTokenId(
        tokenId,
        ergoTreeTemplate,
        offset,
        limit
      ).stream
        .translate(liftK)

    def getAllMainUnspentBuyOrderByTokenId(
      tokenId: TokenId,
      ergoTreeTemplate: HexString,
      offset: Int,
      limit: Int
    ): Stream[D, ExtendedOutput] =
      QS.getMainUnspentBuyOrderByTokenId(
        tokenId,
        ergoTreeTemplate,
        offset,
        limit
      ).stream
        .translate(liftK)

    def getAllLike(query: String): D[List[Address]] =
      QS.getAllLike(query).to[List].liftConnectionIO

    def sumOfAllUnspentOutputsSince(ts: Long): D[BigDecimal] =
      QS.sumOfAllUnspentOutputsSince(ts).unique.liftConnectionIO

    def estimatedOutputsSince(ts: Long)(genesisAddress: Address): D[BigDecimal] =
      QS.estimatedOutputsSince(ts)(genesisAddress).unique.liftConnectionIO

    def updateChainStatusByHeaderId(headerId: Id, newChainStatus: Boolean): D[Unit] =
      QS.updateChainStatusByHeaderId(headerId, newChainStatus).run.void.liftConnectionIO

    def getAllMainUnspent(minHeight: Int, maxHeight: Int): Stream[D, Output] =
      QS.getAllMainUnspent(minHeight, maxHeight).stream.translate(liftK)

    def getAllByTokenId(tokenId: TokenId, offset: Int, limit: Int): Stream[D, ExtendedOutput] =
      QS.getAllByTokenId(tokenId, offset, limit).stream.translate(liftK)

    def getUnspentByTokenId(tokenId: TokenId, offset: Int, limit: Int): Stream[D, Output] =
      QS.getUnspentByTokenId(tokenId, offset, limit).stream.translate(liftK)
  }
}
