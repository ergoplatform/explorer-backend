package org.ergoplatform.explorer.services

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.~>
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import mouse.anyf._
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories.{BlockInfoRepo, HeaderRepo}

/** A service providing an access to the blockchain data.
  */
trait BlockchainService[F[_]] {

  /** Get height of the best block.
    */
  def getBestHeight: F[Int]
}

object BlockchainService {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO](
    xa: D ~> F
  ): F[BlockchainService[F]] =
    Sync[F]
      .suspend(Slf4jLogger.create)
      .map { implicit logger =>
        new Live(HeaderRepo[D], BlockInfoRepo[D])(xa)
      }

  final private class Live[F[_]: Sync: Logger, D[_]](
    headerRepo: HeaderRepo[D],
    blockInfoRepo: BlockInfoRepo[D]
  )(xa: D ~> F)
    extends BlockchainService[F] {

    def getBestHeight: F[Int] =
      (headerRepo.getBestHeight ||> xa)
        .flatTap(h => Logger[F].trace(s"Reading best height from db: $h"))
  }
}
