package org.ergoplatform.explorer.persistence.repositories

import cats.effect.Sync
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.ergoplatform.explorer.Id

/** Provide write access to all tables affected by the fork occurrence.
  */
trait ForkAffectedEntitiesRepo[F[_]] {

  /** Update chain status of all affected entities related to a header with a given `id`.
    */
  def updateChainStatusByHeaderId(id: Id)(newChainStatus: Boolean): F[Unit]
}

object ForkAffectedEntitiesRepo {

  final class Live[F[_]: Sync](xa: Transactor[F]) extends ForkAffectedEntitiesRepo[F] {

    import org.ergoplatform.explorer.persistence.queries.{HeaderQuerySet => HQS}
    import org.ergoplatform.explorer.persistence.queries.{OutputQuerySet => OQS}
    import org.ergoplatform.explorer.persistence.queries.{InputQuerySet => IQS}

    def updateChainStatusByHeaderId(id: Id)(newChainStatus: Boolean): F[Unit] = {
      val txn = for {
        _ <- HQS.updateChainStatusById(id)(newChainStatus)
        _ <- OQS.updateChainStatusByHeaderId(id)(newChainStatus)
        _ <- IQS.updateChainStatusByHeaderId(id)(newChainStatus)
      } yield ()
      txn.transact(xa)
    }
  }
}
