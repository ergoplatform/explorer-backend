package org.ergoplatform.explorer.db.repositories

import cats.Applicative
import cats.data.NonEmptyList
import org.ergoplatform.explorer.db.models.Output
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer.db.repositories.TestOutputRepo.Source
import org.ergoplatform.explorer._

final class TestOutputRepo[F[_]: Applicative](val source: Source)
  extends OutputRepo[F, fs2.Stream] {

  override def insert(output: Output): F[Unit] = ???

  override def insertMany(outputs: scala.List[Output]): F[Unit] = ???

  override def getByBoxId(boxId: BoxId): F[Option[ExtendedOutput]] = ???

  override def getAllByErgoTree(ergoTree: HexString): F[scala.List[ExtendedOutput]] = ???

  override def getByErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int
  ): fs2.Stream[F, ExtendedOutput] = ???

  override def getAllMainUnspentByErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int
  ): fs2.Stream[F, ExtendedOutput] = ???

  override def sumOfAllMainUnspentByErgoTree(ergoTree: HexString): F[Long] = ???

  override def getAllMainUnspentIdsByErgoTree(ergoTree: HexString): F[List[BoxId]] = ???

  override def getAllMainUnspentByErgoTreeTemplate(
    ergoTreeTemplate: HexString,
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

  override def getAllMainUnspentSellOrderByTokenId(
    tokenId: TokenId,
    ergoTreeTemplate: HexString,
    offset: Int,
    limit: Int
  ): fs2.Stream[F, ExtendedOutput] = fs2.Stream.emits(source.sellOrders)

  override def getAllMainUnspentBuyOrderByTokenId(
    tokenId: TokenId,
    ergoTreeTemplate: HexString,
    offset: Int,
    limit: Int
  ): fs2.Stream[F, ExtendedOutput] = fs2.Stream.emits(source.buyOrders)
}

object TestOutputRepo {

  final case class Source(
    sellOrders: List[ExtendedOutput],
    buyOrders: List[ExtendedOutput]
  )
}
