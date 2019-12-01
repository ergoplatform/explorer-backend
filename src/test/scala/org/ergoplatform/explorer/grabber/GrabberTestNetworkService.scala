package org.ergoplatform.explorer.grabber

import cats.Applicative
import cats.syntax.applicative._
import org.ergoplatform.explorer
import org.ergoplatform.explorer.grabber.GrabberTestNetworkService.Source
import org.ergoplatform.explorer.protocol.models.{ApiFullBlock, ApiTransaction}
import org.ergoplatform.explorer.services.ErgoNetworkService

final class GrabberTestNetworkService[F[_]: Applicative](var source: Source)
  extends ErgoNetworkService[F, fs2.Stream[F, *]] {

  def getBestHeight: F[Int] =
    source.blocksStorage.maxBy(_._1)._1.pure[F]

  def getBlockIdsAtHeight(height: Int): F[List[explorer.Id]] =
    source.blocksStorage.get(height).toList.flatten.map(_.header.id).pure[F]

  def getFullBlockById(id: explorer.Id): F[Option[ApiFullBlock]] =
    source.blocksStorage.values.flatten.find(_.header.id == id).pure[F]

  def getUnconfirmedTransactions: fs2.Stream[F, ApiTransaction] = ???
}

object GrabberTestNetworkService {

  final case class Source(blocks: List[ApiFullBlock]) {

    lazy val blocksStorage: Map[Int, List[ApiFullBlock]] =
      blocks
        .groupBy(_.header.height)
        .map { case (k, vls) => k -> vls.sortBy(_.header.mainChain) }
  }
}
