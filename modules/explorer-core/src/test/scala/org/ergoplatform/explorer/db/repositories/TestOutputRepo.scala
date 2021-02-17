package org.ergoplatform.explorer.db.repositories

import cats.Applicative
import cats.data.NonEmptyList
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.db.models.Output
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer.db.repositories.TestOutputRepo.Source

final class TestOutputRepo[F[_]: Applicative](val source: Source) extends OutputRepo[F, fs2.Stream] {

  override def insert(output: Output): F[Unit] = ???

  override def insertMany(outputs: scala.List[Output]): F[Unit] = ???

  override def getByBoxId(boxId: BoxId): F[Option[ExtendedOutput]] = ???

  override def getAllByErgoTree(
    ergoTree: HexString,
    minConfirmations: Int
  ): F[scala.List[ExtendedOutput]] = ???

  override def streamAllByErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int
  ): fs2.Stream[F, ExtendedOutput] = ???

  override def streamUnspentByErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int
  ): fs2.Stream[F, ExtendedOutput] = ???

  override def sumUnspentByErgoTree(ergoTree: HexString, maxHeight: Int): F[Long] = ???

  override def getAllMainUnspentIdsByErgoTree(ergoTree: HexString): F[List[BoxId]] = ???

  def sumAllByErgoTree(ergoTree: HexString, minConfirmations: Int): F[Long] = ???

  def streamAllByErgoTreeTemplateHash(hash: ErgoTreeTemplateHash, offset: Int, limit: Int): fs2.Stream[F, ExtendedOutput] =
    ???

  def streamUnspentByErgoTreeTemplateHash(hash: ErgoTreeTemplateHash, offset: Int, limit: Int): fs2.Stream[F, Output] = ???

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
    txsId: NonEmptyList[TxId]
  ): F[scala.List[ExtendedOutput]] = ???

  override def getAllLike(
    query: String
  ): F[scala.List[Address]] = ???

  override def sumOfAllUnspentOutputsSince(ts: Long): F[BigDecimal] = ???

  override def estimatedOutputsSince(ts: Long)(
    genesisAddress: Address
  ): F[BigDecimal] = ???

  override def updateChainStatusByHeaderId(headerId: Id, newChainStatus: Boolean): F[Unit] = ???

  override def balanceStatsMain(offset: Int, limit: Int): F[List[(Address, Long)]] = ???

  override def totalAddressesMain: F[Int] = ???

  override def getAllMainUnspent(minHeight: Int, maxHeight: Int): fs2.Stream[F, Output] = ???

  override def getAllByTokenId(tokenId: TokenId, offset: Int, limit: Int): fs2.Stream[F, ExtendedOutput] = ???

  override def getUnspentByTokenId(tokenId: TokenId, offset: Int, limit: Int): fs2.Stream[F, Output] = ???

  override def searchAll(templateHash: ErgoTreeTemplateHash, constants: Option[NonEmptyList[(Int, String)]], offset: Int, limit: Int): fs2.Stream[F, ExtendedOutput] = ???
}

object TestOutputRepo {

  final case class Source(
    sellOrders: List[ExtendedOutput],
    buyOrders: List[ExtendedOutput]
  )
}
