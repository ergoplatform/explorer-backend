package org.ergoplatform.explorer.persistence.repositories

import cats.effect.Sync
import cats.implicits._
import doobie.util.transactor.Transactor
import doobie.implicits._
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.persistence.models.AdProof

/** [[AdProof]] data access operations.
  */
trait AdProofRepo[F[_]] {

  /** Put a given `proof` to persistence.
    */
  def insert(proof: AdProof): F[Unit]

  /** Get proof related to a header with a given `headerId`.
    */
  def getByHeaderId(headerId: Id): F[Option[AdProof]]
}

object AdProofRepo {

  final class Live[F[_]: Sync](xa: Transactor[F]) extends AdProofRepo[F] {

    import org.ergoplatform.explorer.persistence.queries.{AdProofQuerySet => QS}

    def insert(proof: AdProof): F[Unit] =
      QS.insert(proof).transact(xa).as(())

    def getByHeaderId(headerId: Id): F[Option[AdProof]] =
      QS.getByHeaderId(headerId).transact(xa)
  }
}
