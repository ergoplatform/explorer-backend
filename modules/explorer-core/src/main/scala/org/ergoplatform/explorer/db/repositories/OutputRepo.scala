package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import doobie.free.implicits._
import doobie.refined.implicits._
import doobie.util.log.LogHandler
import fs2.Stream
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.constraints.OrderingString
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.doobieInstances._
import org.ergoplatform.explorer.db.models.Output
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._

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
  def getAllByErgoTree(ergoTree: HexString, maxHeight: Int): D[List[ExtendedOutput]]

  /** Get all unspent main-chain outputs with a given `ergoTree` from persistence.
    */
  def getAllMainUnspentIdsByErgoTree(ergoTree: HexString): D[List[BoxId]]

  /** Get total amount of all main-chain outputs with a given `ergoTree`.
    */
  def sumAllByErgoTree(ergoTree: HexString, maxHeight: Int): D[Long]

  /** Get total amount of all unspent main-chain outputs with a given `ergoTree`.
    */
  def sumUnspentByErgoTree(ergoTree: HexString, maxHeight: Int): D[Long]

  /** Get balances of all addresses in the network.
    */
  def balanceStatsMain(offset: Int, limit: Int): D[List[(Address, Long)]]

  /** Get total number of addresses in the network.
    */
  def totalAddressesMain: D[Int]

  /** Get outputs with a given `ergoTree` from persistence.
    */
  def streamAllByErgoTree(ergoTree: HexString, offset: Int, limit: Int): S[D, ExtendedOutput]

  /** Count outputs with a given `ergoTree` from persistence.
    */
  def countAllByErgoTree(ergoTree: HexString): D[Int]

  /** Get unspent main-chain outputs with a given `ergoTree` from persistence.
    */
  def streamUnspentByErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int,
    ord: OrderingString
  ): S[D, ExtendedOutput]

  /** Count unspent main-chain outputs with a given `ergoTree` from persistence.
    */
  def countUnspentByErgoTree(ergoTree: HexString): D[Int]

  /** Get all main-chain outputs that are protected with given ergo tree template.
    */
  def streamAllByErgoTreeTemplateHash(
    hash: ErgoTreeTemplateHash,
    offset: Int,
    limit: Int
  ): S[D, ExtendedOutput]

  /** Count main-chain outputs that are protected with given ergo tree template.
    */
  def countAllByErgoTreeTemplateHash(hash: ErgoTreeTemplateHash): D[Int]

  /** Get all unspent main-chain outputs that are protected with given ergo tree template.
    */
  def streamUnspentByErgoTreeTemplateHash(
    hash: ErgoTreeTemplateHash,
    offset: Int,
    limit: Int
  ): S[D, Output]

  /** Count unspent main-chain outputs that are protected with given ergo tree template.
    */
  def countUnspentByErgoTreeTemplateHash(hash: ErgoTreeTemplateHash): D[Int]

  def streamUnspentByErgoTreeTemplateHashAndTokenId(
    hash: ErgoTreeTemplateHash,
    tokenId: TokenId,
    offset: Int,
    limit: Int
  ): Stream[D, ExtendedOutput]

  /** Get all main-chain outputs that are protected with given ergo tree template.
    */
  def streamAllByErgoTreeTemplateHashByEpochs(
    hash: ErgoTreeTemplateHash,
    minHeight: Int,
    maxHeight: Int
  ): S[D, ExtendedOutput]

  /** Get all unspent main-chain outputs that are protected with given ergo tree template.
    */
  def streamUnspentByErgoTreeTemplateHashByEpochs(
    hash: ErgoTreeTemplateHash,
    minHeight: Int,
    maxHeight: Int
  ): S[D, Output]

  /** Get all outputs related to a given `txId`.
    */
  def getAllByTxId(txId: TxId): D[List[ExtendedOutput]]

  /** Get all outputs related to a given list of `txId`.
    */
  def getAllByTxIds(txsId: NonEmptyList[TxId], narrowByAddress: Option[Address]): D[List[ExtendedOutput]]

  /** Get all addresses matching the given `query`.
    */
  def getAllLike(query: String): D[List[Address]]

  def sumOfAllUnspentOutputsSince(ts: Long): D[BigDecimal]

  def estimatedOutputsSince(ts: Long)(genesisAddress: Address): D[BigDecimal]

  /** Update main_chain status for all outputs related to given `headerId`.
    */
  def updateChainStatusByHeaderId(headerId: BlockId, newChainStatus: Boolean): D[Unit]

  /** Get all unspent outputs appeared in the main chain after `minHeight`.
    */
  def streamAllUnspent(minHeight: Int, maxHeight: Int): S[D, Output]

  /** Get all unspent outputs appeared in the blockchain after an output at a given global index `minGix` (inclusively).
    */
  def streamAllUnspent(minGix: Long, limit: Int): S[D, Output]

  /** Get all outputs appeared in the blockchain after an output at a given global index `minGix` (inclusively).
    */
  def streamAll(minGix: Long, limit: Int): S[D, Output]

  def getAllByTokenId(tokenId: TokenId, offset: Int, limit: Int): S[D, ExtendedOutput]

  def countAllByTokenId(tokenId: TokenId): D[Int]

  def getUnspentByTokenId(tokenId: TokenId, offset: Int, limit: Int, ordering: OrderingString): S[D, Output]

  def countUnspentByTokenId(tokenId: TokenId): D[Int]

  def searchAll(
    templateHash: ErgoTreeTemplateHash,
    registers: Option[NonEmptyList[(RegisterId, String)]],
    constants: Option[NonEmptyList[(Int, String)]],
    assets: Option[NonEmptyList[TokenId]],
    offset: Int,
    limit: Int
  ): S[D, ExtendedOutput]

  def countAll(
    templateHash: ErgoTreeTemplateHash,
    registers: Option[NonEmptyList[(RegisterId, String)]],
    constants: Option[NonEmptyList[(Int, String)]],
    assets: Option[NonEmptyList[TokenId]]
  ): D[Int]

  def searchUnspent(
    templateHash: ErgoTreeTemplateHash,
    registers: Option[NonEmptyList[(RegisterId, String)]],
    constants: Option[NonEmptyList[(Int, String)]],
    assets: Option[NonEmptyList[TokenId]],
    offset: Int,
    limit: Int
  ): S[D, Output]

  def countUnspent(
    templateHash: ErgoTreeTemplateHash,
    registers: Option[NonEmptyList[(RegisterId, String)]],
    constants: Option[NonEmptyList[(Int, String)]],
    assets: Option[NonEmptyList[TokenId]]
  ): D[Int]

  def searchUnspentByAssetsUnion(
    templateHash: ErgoTreeTemplateHash,
    assets: List[TokenId],
    offset: Int,
    limit: Int
  ): S[D, Output]

  def countUnspentByAssetsUnion(
    templateHash: ErgoTreeTemplateHash,
    assets: List[TokenId]
  ): D[Int]
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

    def getAllByErgoTree(ergoTree: HexString, maxHeight: Int): D[List[ExtendedOutput]] =
      QS.getMainByErgoTree(ergoTree, offset = 0, limit = Int.MaxValue, maxHeight = maxHeight)
        .to[List]
        .liftConnectionIO

    def countAllByErgoTree(ergoTree: HexString): D[Int] =
      QS.countAllByErgoTree(ergoTree).unique.liftConnectionIO

    def streamAllByErgoTree(
      ergoTree: HexString,
      offset: Int,
      limit: Int
    ): Stream[D, ExtendedOutput] =
      QS.getMainByErgoTree(ergoTree, offset, limit).stream.translate(liftK)

    def getAllMainUnspentIdsByErgoTree(ergoTree: HexString): D[List[BoxId]] =
      QS.getAllMainUnspentIdsByErgoTree(ergoTree)
        .to[List]
        .liftConnectionIO

    def sumAllByErgoTree(ergoTree: HexString, maxHeight: Int): D[Long] =
      QS.sumAllByErgoTree(ergoTree, maxHeight).unique.liftConnectionIO

    def sumUnspentByErgoTree(ergoTree: HexString, maxHeight: Int): D[Long] =
      QS.sumUnspentByErgoTree(ergoTree, maxHeight).unique.liftConnectionIO

    def balanceStatsMain(offset: Int, limit: Int): D[List[(Address, Long)]] =
      QS.balanceStatsMain(offset, limit).to[List].liftConnectionIO

    def totalAddressesMain: D[Int] =
      QS.totalAddressesMain.unique.liftConnectionIO

    def streamUnspentByErgoTree(
      ergoTree: HexString,
      offset: Int,
      limit: Int,
      ordering: OrderingString
    ): Stream[D, ExtendedOutput] =
      QS.getMainUnspentByErgoTree(ergoTree, offset, limit, ordering).stream.translate(liftK)

    def countUnspentByErgoTree(ergoTree: HexString): D[Int] =
      QS.countUnspentByErgoTree(ergoTree).unique.liftConnectionIO

    def streamAllByErgoTreeTemplateHash(
      hash: ErgoTreeTemplateHash,
      offset: Int,
      limit: Int
    ): Stream[D, ExtendedOutput] =
      QS.getAllByErgoTreeTemplateHash(hash, offset, limit).stream.translate(liftK)

    def countAllByErgoTreeTemplateHash(hash: ErgoTreeTemplateHash): D[Int] =
      QS.countAllByErgoTreeTemplateHash(hash).unique.liftConnectionIO

    def streamUnspentByErgoTreeTemplateHash(
      hash: ErgoTreeTemplateHash,
      offset: Int,
      limit: Int
    ): Stream[D, Output] =
      QS.getUnspentByErgoTreeTemplateHash(hash, offset, limit).stream.translate(liftK)

    def countUnspentByErgoTreeTemplateHash(hash: ErgoTreeTemplateHash): D[Int] =
      QS.countUnspentByErgoTreeTemplateHash(hash).unique.liftConnectionIO

    def streamAllByErgoTreeTemplateHashByEpochs(
      hash: ErgoTreeTemplateHash,
      minHeight: Int,
      maxHeight: Int
    ): Stream[D, ExtendedOutput] =
      QS.getAllByErgoTreeTemplateHashByEpochs(hash, minHeight, maxHeight).stream.translate(liftK)

    def streamUnspentByErgoTreeTemplateHashByEpochs(
      hash: ErgoTreeTemplateHash,
      minHeight: Int,
      maxHeight: Int
    ): Stream[D, Output] =
      QS.getUnspentByErgoTreeTemplateHashByEpochs(hash, minHeight, maxHeight).stream.translate(liftK)

    def streamUnspentByErgoTreeTemplateHashAndTokenId(
      hash: ErgoTreeTemplateHash,
      tokenId: TokenId,
      offset: Int,
      limit: Int
    ): Stream[D, ExtendedOutput] =
      QS.getUnspentByErgoTreeTemplateHashAndTokenId(
        hash,
        tokenId,
        offset,
        limit
      ).stream
        .translate(liftK)

    def getAllByTxId(txId: TxId): D[List[ExtendedOutput]] =
      QS.getAllByTxId(txId).to[List].liftConnectionIO

    def getAllByTxIds(txIds: NonEmptyList[TxId], narrowByAddress: Option[Address]): D[List[ExtendedOutput]] =
      narrowByAddress.fold(QS.getAllByTxIds(txIds))(QS.getAllByTxIds(txIds, _)).to[List].liftConnectionIO

    def getAllLike(query: String): D[List[Address]] =
      QS.getAllLike(query).to[List].liftConnectionIO

    def sumOfAllUnspentOutputsSince(ts: Long): D[BigDecimal] =
      QS.sumOfAllUnspentOutputsSince(ts).unique.liftConnectionIO

    def estimatedOutputsSince(ts: Long)(genesisAddress: Address): D[BigDecimal] =
      QS.estimatedOutputsSince(ts)(genesisAddress).unique.liftConnectionIO

    def updateChainStatusByHeaderId(headerId: BlockId, newChainStatus: Boolean): D[Unit] =
      QS.updateChainStatusByHeaderId(headerId, newChainStatus).run.void.liftConnectionIO

    def streamAllUnspent(minHeight: Int, maxHeight: Int): Stream[D, Output] =
      QS.getUnspent(minHeight, maxHeight).stream.translate(liftK)

    def streamAllUnspent(minGix: Long, limit: Int): Stream[D, Output] =
      QS.getUnspent(minGix, limit).stream.translate(liftK)

    def streamAll(minGix: Long, limit: Int): Stream[D, Output] =
      QS.getAll(minGix, limit).stream.translate(liftK)

    def getAllByTokenId(tokenId: TokenId, offset: Int, limit: Int): Stream[D, ExtendedOutput] =
      QS.getAllByTokenId(tokenId, offset, limit).stream.translate(liftK)

    def countAllByTokenId(tokenId: TokenId): D[Int] =
      QS.countAllByTokenId(tokenId).unique.liftConnectionIO

    def getUnspentByTokenId(tokenId: TokenId, offset: Int, limit: Int, ordering: OrderingString): Stream[D, Output] =
      QS.getUnspentByTokenId(tokenId, offset, limit, ordering).stream.translate(liftK)

    def countUnspentByTokenId(tokenId: TokenId): D[Int] =
      QS.countUnspentByTokenId(tokenId).unique.liftConnectionIO

    def searchAll(
      templateHash: ErgoTreeTemplateHash,
      registers: Option[NonEmptyList[(RegisterId, String)]],
      constants: Option[NonEmptyList[(Int, String)]],
      assets: Option[NonEmptyList[TokenId]],
      offset: Int,
      limit: Int
    ): Stream[D, ExtendedOutput] =
      QS.searchAll(templateHash, registers, constants, assets, offset, limit).stream.translate(liftK)

    def countAll(
      templateHash: ErgoTreeTemplateHash,
      registers: Option[NonEmptyList[(RegisterId, String)]],
      constants: Option[NonEmptyList[(Int, String)]],
      assets: Option[NonEmptyList[TokenId]]
    ): D[Int] =
      QS.countAll(templateHash, registers, constants, assets).unique.liftConnectionIO

    def searchUnspent(
      templateHash: ErgoTreeTemplateHash,
      registers: Option[NonEmptyList[(RegisterId, String)]],
      constants: Option[NonEmptyList[(Int, String)]],
      assets: Option[NonEmptyList[TokenId]],
      offset: Int,
      limit: Int
    ): Stream[D, Output] =
      QS.searchUnspent(templateHash, registers, constants, assets, offset, limit).stream.translate(liftK)

    def countUnspent(
      templateHash: ErgoTreeTemplateHash,
      registers: Option[NonEmptyList[(RegisterId, String)]],
      constants: Option[NonEmptyList[(Int, String)]],
      assets: Option[NonEmptyList[TokenId]]
    ): D[Int] =
      QS.countUnspent(templateHash, registers, constants, assets).unique.liftConnectionIO

    def searchUnspentByAssetsUnion(
      templateHash: ErgoTreeTemplateHash,
      assets: List[TokenId],
      offset: Int,
      limit: Int
    ): Stream[D, Output] =
      QS.searchUnspentByAssetsUnion(templateHash, assets, offset, limit).stream.translate(liftK)

    def countUnspentByAssetsUnion(templateHash: ErgoTreeTemplateHash, assets: List[TokenId]): D[Int] =
      QS.countUnspentByAssetsUnion(templateHash, assets).unique.liftConnectionIO
  }
}
