package org.ergoplatform.explorer.db.repositories

import cats.effect.Sync
import cats.tagless.syntax.functorK._
import derevo.derive
import doobie.ConnectionIO
import doobie.util.log.LogHandler
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import tofu.higherKind.derived.representableK
import tofu.syntax.monadic._

@derive(representableK)
trait StatsRepo[D[_]] {
  def countUniqueAddrs: D[Long]
}

object StatsRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[StatsRepo[D]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      (new Live).mapK(LiftConnectionIO[D].liftConnectionIOK)
    }

  final class Live(implicit lh: LogHandler) extends StatsRepo[ConnectionIO] {

    import org.ergoplatform.explorer.db.queries.{StatsQuerySet => QS}

    def countUniqueAddrs: ConnectionIO[Long] = QS.countUniqueAddrs.unique
  }
}
