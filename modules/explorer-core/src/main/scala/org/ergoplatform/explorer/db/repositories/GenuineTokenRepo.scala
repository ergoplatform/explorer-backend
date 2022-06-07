package org.ergoplatform.explorer.db.repositories

import cats.Monad
import cats.effect.Sync
import cats.tagless.syntax.functorK._
import derevo.derive
import doobie.ConnectionIO
import doobie.free.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.explorer.{TokenId, TokenName}
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.GenuineToken
import tofu.higherKind.derived.representableK
import tofu.syntax.monadic._

@derive(representableK)
trait GenuineTokenRepo[D[_]] {

  def insertUnsafe(token: GenuineToken): D[Unit]

  def insert(token: GenuineToken): D[Unit]

  def insertMany(tokens: List[GenuineToken]): D[Unit]

  def get(id: TokenId): D[Option[GenuineToken]]

  def get(id: TokenId, name: String): D[Option[GenuineToken]]

  def getByName(tokenName: String): D[List[GenuineToken]]

  def getByNameAndUnique(tokenName: TokenName, unique: Boolean): D[List[GenuineToken]]

  def getByNameAndUniqueOP(tokenName: TokenName, unique: Boolean): D[Option[List[GenuineToken]]]

  def getAll(offset: Int, limit: Int): D[List[GenuineToken]]

  def countAll: D[Int]
}

object GenuineTokenRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO: Monad]: F[GenuineTokenRepo[D]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      (new Live).mapK(LiftConnectionIO[D].liftConnectionIOK)
    }

  final class Live(implicit lh: LogHandler) extends GenuineTokenRepo[ConnectionIO] {

    import org.ergoplatform.explorer.db.queries.{GenuineTokenQuerySet => QS}

    // uniqueName must be one
    // if tokeName is already in use, uniqueName cannot be inserted
    override def insert(token: GenuineToken): ConnectionIO[Unit] =
      getByName(token.tokenName).flatMap { x =>
        if (x.isEmpty) insertUnsafe(token)
        else {
          if (x.exists(_.uniqueName == true)) ().pure[ConnectionIO]
          else {
            if (token.uniqueName) ().pure[ConnectionIO]
            else insertUnsafe(token)
          }
        }
      }

    override def insertUnsafe(token: GenuineToken): ConnectionIO[Unit] = QS.insertNoConflict(token).void

    override def insertMany(tokens: List[GenuineToken]): ConnectionIO[Unit] = QS.insertManyNoConflict(tokens).void

    override def get(id: TokenId): ConnectionIO[Option[GenuineToken]] = QS.get(id).option

    override def getAll(offset: Int, limit: Int): ConnectionIO[List[GenuineToken]] = QS.getAll(offset, limit).to[List]

    override def countAll: ConnectionIO[Int] = QS.countAll.unique

    // can only be one genuine token with a tokenName and uniqueName=true
    override def getByNameAndUnique(tokenName: TokenName, unique: Boolean): ConnectionIO[List[GenuineToken]] =
      QS.get(tokenName, unique).to[List]

    override def getByNameAndUniqueOP(tokenName: TokenName, unique: Boolean): ConnectionIO[Option[List[GenuineToken]]] =
      QS.get(tokenName, unique).to[List].map {
        case Nil     => None
        case List()  => None
        case x :: xs => Some(x :: xs)
      }

    override def get(id: TokenId, name: String): ConnectionIO[Option[GenuineToken]] = QS.get(id, name).option

    override def getByName(tokenName: String): ConnectionIO[List[GenuineToken]] = QS.get(tokenName).to[List]

  }
}
