package org.ergoplatform.explorer.db.repositories

import cats.effect.Sync
import doobie.free.implicits._
import doobie.refined.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.{BoxRegister, ScriptConstant}
import org.ergoplatform.explorer.db.syntax.liftConnectionIO._
import org.ergoplatform.explorer.db.doobieInstances._
import tofu.syntax.monadic._

trait ScriptConstantsRepo[D[_]] {

  /** Put a given `register` to persistence.
    */
  def insert(const: ScriptConstant): D[Unit]

  /** Persist a given list of `registers`.
    */
  def insertMany(consts: List[ScriptConstant]): D[Unit]
}

object ScriptConstantsRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[ScriptConstantsRepo[D]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      new Live[D]
    }

  final private class Live[D[_]: LiftConnectionIO](implicit lh: LogHandler) extends ScriptConstantsRepo[D] {

    import org.ergoplatform.explorer.db.queries.{ScriptConstantsQuerySet => QS}

    def insert(const: ScriptConstant): D[Unit] =
      QS.insertNoConflict(const).void.liftConnectionIO

    def insertMany(consts: List[ScriptConstant]): D[Unit] =
      QS.insertManyNoConflict(consts).void.liftConnectionIO
  }
}
