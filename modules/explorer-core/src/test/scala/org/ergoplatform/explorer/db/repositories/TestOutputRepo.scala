package org.ergoplatform.explorer.db.repositories

import cats.Applicative
import cats.data.NonEmptyList
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.constraints.OrderingString
import org.ergoplatform.explorer.db.models.Output
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer.db.repositories.TestOutputRepo.Source

final class TestOutputRepo[F[_]: Applicative](val source: Source) extends OutputRepo[F, fs2.Stream] {

  override def insert(output: Output): F[Unit] = ???

  def countAllByErgoTree(ergoTree: HexString): F[Int] = ???

  def countUnspentByErgoTree(ergoTree: HexString): F[Int] = ???

  def countAllByErgoTreeTemplateHash(hash: ErgoTreeTemplateHash): F[Int] = ???

  def countUnspentByErgoTreeTemplateHash(hash: ErgoTreeTemplateHash): F[Int] = ???

  override def insertMany(outputs: scala.List[Output]): F[Unit] = ???

  override def getByBoxId(boxId: BoxId): F[Option[ExtendedOutput]] = ???

  def streamAllUnspent(minGix: Long, limit: Int): fs2.Stream[F, Output] = ???

  override def streamAll(minGix: Long, limit: Int): fs2.Stream[F, Output] = ???

  override def getAllByErgoTree(
    ergoTree: HexString,
    minConfirmations: Int
  ): F[scala.List[ExtendedOutput]] = ???

  override def streamAllByErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int
  ): fs2.Stream[F, ExtendedOutput] = ???

  def countAllByTokenId(tokenId: TokenId): F[Int] = ???

  def countUnspentByTokenId(tokenId: TokenId): F[Int] = ???

  def countAll(templateHash: ErgoTreeTemplateHash, registers: Option[NonEmptyList[(RegisterId, String)]], constants: Option[NonEmptyList[(Int, String)]], assets: Option[NonEmptyList[TokenId]]): F[Int] = ???

  override def sumUnspentByErgoTree(ergoTree: HexString, maxHeight: Int): F[Long] = ???

  override def getAllMainUnspentIdsByErgoTree(ergoTree: HexString): F[List[BoxId]] = ???

  def sumAllByErgoTree(ergoTree: HexString, minConfirmations: Int): F[Long] = ???

  def streamAllByErgoTreeTemplateHash(
    hash: ErgoTreeTemplateHash,
    offset: Int,
    limit: Int
  ): fs2.Stream[F, ExtendedOutput] =
    ???

  def streamUnspentByErgoTreeTemplateHash(hash: ErgoTreeTemplateHash, offset: Int, limit: Int): fs2.Stream[F, Output] =
    ???

  def streamAllByErgoTreeTemplateHashByEpochs(
    hash: ErgoTreeTemplateHash,
    minHeight: Int,
    maxHeight: Int
  ): fs2.Stream[F, ExtendedOutput] = ???

  def streamUnspentByErgoTreeTemplateHashByEpochs(
    hash: ErgoTreeTemplateHash,
    minHeight: Int,
    maxHeight: Int
  ): fs2.Stream[F, Output] = ???

  def streamUnspentByErgoTreeTemplateHashAndTokenId(
    hash: ErgoTreeTemplateHash,
    tokenId: TokenId,
    offset: Int,
    limit: Int
  ): fs2.Stream[F, ExtendedOutput] = ???

  override def getAllByTxId(
    txId: TxId
  ): F[scala.List[ExtendedOutput]] = ???

  override def getAllByTxIds(
    txsId: NonEmptyList[TxId],
    narrowByAddress: Option[Address]
  ): F[scala.List[ExtendedOutput]] = ???

  override def getAllLike(
    query: String
  ): F[scala.List[Address]] = ???

  override def sumOfAllUnspentOutputsSince(ts: Long): F[BigDecimal] = ???

  override def estimatedOutputsSince(ts: Long)(
    genesisAddress: Address
  ): F[BigDecimal] = ???

  override def updateChainStatusByHeaderId(headerId: BlockId, newChainStatus: Boolean): F[Unit] = ???

  override def balanceStatsMain(offset: Int, limit: Int): F[List[(Address, Long)]] = ???

  override def totalAddressesMain: F[Int] = ???

  override def streamAllUnspent(minHeight: Int, maxHeight: Int): fs2.Stream[F, Output] = ???

  override def getAllByTokenId(tokenId: TokenId, offset: Int, limit: Int): fs2.Stream[F, ExtendedOutput] = ???

  override def getUnspentByTokenId(tokenId: TokenId, offset: Int, limit: Int, ordering: OrderingString): fs2.Stream[F, Output] = ???

  override def searchAll(
    templateHash: ErgoTreeTemplateHash,
    registers: Option[NonEmptyList[(RegisterId, String)]],
    constants: Option[NonEmptyList[(Int, String)]],
    assets: Option[NonEmptyList[TokenId]],
    offset: Int,
    limit: Int
  ): fs2.Stream[F, ExtendedOutput] = ???

  def searchUnspent(templateHash: ErgoTreeTemplateHash, registers: Option[NonEmptyList[(RegisterId, String)]], constants: Option[NonEmptyList[(Int, String)]], assets: Option[NonEmptyList[TokenId]], offset: Int, limit: Int): fs2.Stream[F, Output] = ???

  def countUnspent(templateHash: ErgoTreeTemplateHash, registers: Option[NonEmptyList[(RegisterId, String)]], constants: Option[NonEmptyList[(Int, String)]], assets: Option[NonEmptyList[TokenId]]): F[Int] = ???

  def searchUnspentByAssetsUnion(templateHash: ErgoTreeTemplateHash, assets: List[TokenId], offset: Int, limit: Int): fs2.Stream[F, Output] = ???

  def countUnspentByAssetsUnion(templateHash: ErgoTreeTemplateHash, assets: List[TokenId]): F[Int] = ???

  /** Get unspent main-chain outputs with a given `ergoTree` from persistence.
    */
  def streamUnspentByErgoTree(ergoTree: HexString, offset: Int, limit: Int, ord: OrderingString): fs2.Stream[F, ExtendedOutput] = ???
}

object TestOutputRepo {

  final case class Source(
    sellOrders: List[ExtendedOutput],
    buyOrders: List[ExtendedOutput]
  )
}
