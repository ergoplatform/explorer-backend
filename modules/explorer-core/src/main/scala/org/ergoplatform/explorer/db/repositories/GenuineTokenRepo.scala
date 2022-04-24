package org.ergoplatform.explorer.db.repositories

import cats.effect.Sync
import cats.tagless.syntax.functorK._
import derevo.derive
import doobie.ConnectionIO
import doobie.free.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.GenuineToken
import tofu.higherKind.derived.representableK
import tofu.syntax.monadic._

@derive(representableK)
trait GenuineTokenRepo[D[_]] {

  def insert(token: GenuineToken): D[Unit]

  def insertMany(tokens: List[GenuineToken]): D[Unit]

  def get(id: TokenId): D[Option[GenuineToken]]

  def getAll(offset: Int, limit: Int): D[List[GenuineToken]]

  def countAll: D[Int]
}

object GenuineTokenRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[GenuineTokenRepo[D]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      (new Live).mapK(LiftConnectionIO[D].liftConnectionIOK)
    }

  final class Live(implicit lh: LogHandler) extends GenuineTokenRepo[ConnectionIO] {

    import org.ergoplatform.explorer.db.queries.{GenuineTokenQuerySet => QS}

    override def insert(token: GenuineToken): ConnectionIO[Unit] = QS.insertNoConflict(token).void

    override def insertMany(tokens: List[GenuineToken]): ConnectionIO[Unit] = QS.insertManyNoConflict(tokens).void

    override def get(id: TokenId): ConnectionIO[Option[GenuineToken]] = QS.get(id).option

    override def getAll(offset: Int, limit: Int): ConnectionIO[List[GenuineToken]] = QS.getAll(offset, limit).to[List]

    override def countAll: ConnectionIO[Int] = QS.countAll.unique
  }
}
