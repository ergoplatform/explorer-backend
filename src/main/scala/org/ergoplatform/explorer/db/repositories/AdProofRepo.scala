package org.ergoplatform.explorer.db.repositories

import cats.Functor
import cats.syntax.functor._
import doobie.refined.implicits._
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.AdProof
import org.ergoplatform.explorer.db.algebra.syntax.liftConnectionIO._

/** [[AdProof]] data access operations.
  */
trait AdProofRepo[D[_]] {

  /** Put a given `proof` to persistence.
    */
  def insert(proof: AdProof): D[Unit]

  /** Get proof related to a header with a given `headerId`.
    */
  def getByHeaderId(headerId: Id): D[Option[AdProof]]
}

object AdProofRepo {

  def apply[D[_]: LiftConnectionIO: Functor]: AdProofRepo[D] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO: Functor] extends AdProofRepo[D] {

    import org.ergoplatform.explorer.db.queries.{AdProofQuerySet => QS}

    def insert(proof: AdProof): D[Unit] =
      QS.insert(proof).liftConnectionIO.void

    def getByHeaderId(headerId: Id): D[Option[AdProof]] =
      QS.getByHeaderId(headerId).liftConnectionIO
  }
}
