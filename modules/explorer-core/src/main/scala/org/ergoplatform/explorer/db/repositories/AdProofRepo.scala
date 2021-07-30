package org.ergoplatform.explorer.db.repositories

import cats.effect.Sync
import cats.syntax.functor._
import doobie.LogHandler
import doobie.free.implicits._
import doobie.refined.implicits._
import org.ergoplatform.explorer.BlockId
import org.ergoplatform.explorer.db.DoobieLogHandler
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
  def getByHeaderId(headerId: BlockId): D[Option[AdProof]]
}

object AdProofRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[AdProofRepo[D]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler) extends AdProofRepo[D] {

    import org.ergoplatform.explorer.db.queries.{AdProofQuerySet => QS}

    def insert(proof: AdProof): D[Unit] =
      QS.insertNoConflict(proof).void.liftConnectionIO

    def getByHeaderId(headerId: BlockId): D[Option[AdProof]] =
      QS.getByHeaderId(headerId).option.liftConnectionIO
  }
}
