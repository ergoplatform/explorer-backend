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
import org.ergoplatform.explorer.db.models.BlockedToken
import tofu.higherKind.derived.representableK
import tofu.syntax.monadic._

@derive(representableK)
trait BlockedTokenRepo[D[_]] {

  def insert(token: BlockedToken): D[Unit]

  def insertMany(tokens: List[BlockedToken]): D[Unit]

  def get(id: TokenId): D[Option[BlockedToken]]

  def getAll(offset: Int, limit: Int): D[List[BlockedToken]]

  def countAll: D[Int]
}

object BlockedTokenRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[BlockedTokenRepo[D]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      (new Live).mapK(LiftConnectionIO[D].liftConnectionIOK)
    }

  final class Live(implicit lh: LogHandler) extends BlockedTokenRepo[ConnectionIO] {

    import org.ergoplatform.explorer.db.queries.{BlockedTokenQuerySet => QS}

    override def insert(token: BlockedToken): ConnectionIO[Unit] = QS.insertNoConflict(token).void

    override def insertMany(tokens: List[BlockedToken]): ConnectionIO[Unit] = QS.insertManyNoConflict(tokens).void

    override def get(id: TokenId): ConnectionIO[Option[BlockedToken]] = QS.get(id).option

    override def getAll(offset: Int, limit: Int): ConnectionIO[List[BlockedToken]] = QS.getAll(offset, limit).to[List]

    override def countAll: ConnectionIO[Int] = QS.countAll.unique
  }

}
