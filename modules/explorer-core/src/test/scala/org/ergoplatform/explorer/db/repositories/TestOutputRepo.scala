package org.ergoplatform.explorer.db.repositories

import cats.Applicative
import cats.syntax.applicative._
import cats.data.NonEmptyList
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.{Address, BoxId, HexString, TokenId, TxId}
import org.ergoplatform.explorer.db.models.{Asset, Output}
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer.db.repositories.TestOutputRepo.Source
import org.ergoplatform.explorer.syntax.stream._

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
    ergoTree: HexString
  ): F[scala.List[ExtendedOutput]] = ???

  override def getMainUnspentByErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int
  ): fs2.Stream[F, ExtendedOutput] = ???

  override def getAllMainUnspentByErgoTreeTemplate(
    ergoTreeTemplate: HexString
  ): fs2.Stream[F, ExtendedOutput] = ???

  override def getAllByTxId(
    txId: TxId
  ): F[scala.List[ExtendedOutput]] = ???

  override def getAllByTxIds(
    txsId: NonEmptyList[TxId]
  ): F[scala.List[ExtendedOutput]] = ???

  override def searchAddressesBySubstring(
    substring: String
  ): F[scala.List[Address]] = ???

  override def getAllMainUnspentSellOrderByTokenId(
    tokenId: TokenId
  ): fs2.Stream[F, ExtendedOutput] = fs2.Stream.emits(source.sellOrders)

  override def getAllMainUnspentBuyOrderByTokenId(
    tokenId: TokenId
  ): fs2.Stream[F, ExtendedOutput] = ???
}

object TestOutputRepo {

  final case class Source(
    sellOrders: List[ExtendedOutput],
    buyOrders: List[ExtendedOutput]
  ) {

//    def fromOutputs(
//      sellOrdersOuts: List[(Output, Asset)],
//      buyOrders: List[ExtendedOutput]
//    ): Source = {
//      val sellOrdersExtOuts = sellOrdersOuts.map(p => (ExtendedOutput(p._1, None), p._2))
//      new Source(sellOrdersExtOuts, buyOrders)
//    }

  }

}
