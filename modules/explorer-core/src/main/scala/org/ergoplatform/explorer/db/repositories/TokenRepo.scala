package org.ergoplatform.explorer.db.repositories

import cats.effect.Sync
import cats.tagless.syntax.functorK._
import doobie.free.implicits._
import derevo.derive
import doobie.ConnectionIO
import doobie.util.log.LogHandler
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.Token
import tofu.higherKind.derived.representableK
import tofu.syntax.monadic._

@derive(representableK)
trait TokenRepo[D[_]] {

  def insert(token: Token): D[Unit]

  def insertMany(tokens: List[Token]): D[Unit]

  def getAll(offset: Int, limit: Int): D[List[Token]]

  def countAll: D[Int]
}

object TokenRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[TokenRepo[D]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      (new Live).mapK(LiftConnectionIO[D].liftConnectionIOK)
    }

  final class Live(implicit lh: LogHandler) extends TokenRepo[ConnectionIO] {

    import org.ergoplatform.explorer.db.queries.{TokensQuerySet => QS}

    def insert(token: Token): ConnectionIO[Unit] = QS.insert(token).void

    def insertMany(tokens: List[Token]): ConnectionIO[Unit] = QS.insertMany(tokens).void

    def getAll(offset: Int, limit: Int): ConnectionIO[List[Token]] = QS.getAll(offset, limit).to[List]

    def countAll: ConnectionIO[Int] = QS.countAll.unique
  }
}
