package org.ergoplatform.explorer.db.repositories

import cats.effect.Sync
import doobie.free.implicits._
import doobie.refined.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.BoxRegister
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.doobieInstances._
import tofu.syntax.monadic._

trait BoxRegisterRepo[D[_]] {

  /** Put a given `register` to persistence.
    */
  def insert(register: BoxRegister): D[Unit]

  /** Persist a given list of `registers`.
    */
  def insertMany(registers: List[BoxRegister]): D[Unit]
}

object BoxRegisterRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[BoxRegisterRepo[D]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler) extends BoxRegisterRepo[D] {

    import org.ergoplatform.explorer.db.queries.{BoxRegisterQuerySet => QS}

    def insert(register: BoxRegister): D[Unit] =
      QS.insertNoConflict(register).void.liftConnectionIO

    def insertMany(registers: List[BoxRegister]): D[Unit] =
      QS.insertManyNoConflict(registers).void.liftConnectionIO
  }
}
