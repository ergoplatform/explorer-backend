package org.ergoplatform.explorer.db.repositories

import cats.syntax.functor._
import doobie.free.implicits._
import doobie.refined.implicits._
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.AdProof
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._

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

  def apply[D[_]: LiftConnectionIO]: AdProofRepo[D] =
    new Live[D]

  final private class Live[D[_]: LiftConnectionIO] extends AdProofRepo[D] {

    import org.ergoplatform.explorer.db.queries.{AdProofQuerySet => QS}

    def insert(proof: AdProof): D[Unit] =
      QS.insert(proof).void.liftConnectionIO

    def getByHeaderId(headerId: Id): D[Option[AdProof]] =
      QS.getByHeaderId(headerId).option.liftConnectionIO
  }
}
