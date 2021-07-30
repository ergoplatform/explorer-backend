package org.ergoplatform.explorer.indexer

import cats.Applicative
import cats.syntax.applicative._
import org.ergoplatform.explorer.services.ErgoNetwork
import org.ergoplatform.{ErgoLikeTransaction, explorer}
import org.ergoplatform.explorer.indexer.GrabberTestNetwork.Source
import org.ergoplatform.explorer.protocol.models.{ApiFullBlock, ApiNodeInfo, ApiTransaction}

final class GrabberTestNetwork[F[_]: Applicative](val source: Source)
  extends ErgoNetwork[F] {

  def getBestHeight: F[Int] =
    source.blocksStorage.maxBy(_._1)._1.pure[F]

  def getBlockIdsAtHeight(height: Int): F[List[explorer.BlockId]] =
    source.blocksStorage.get(height).toList.flatten.map(_.header.id).pure[F]

  def getFullBlockById(id: explorer.BlockId): F[Option[ApiFullBlock]] =
    source.blocksStorage.values.flatten.find(_.header.id == id).pure[F]

  def getUnconfirmedTransactions: F[List[ApiTransaction]] = ???

  def submitTransaction(tx: ErgoLikeTransaction): F[Unit] = ???

  def getNodeInfo: F[ApiNodeInfo] = ???
}

object GrabberTestNetwork {

  final case class Source(blocks: List[ApiFullBlock]) {

    lazy val blocksStorage: Map[Int, List[ApiFullBlock]] =
      blocks
        .groupBy(_.header.height)
  }
}
