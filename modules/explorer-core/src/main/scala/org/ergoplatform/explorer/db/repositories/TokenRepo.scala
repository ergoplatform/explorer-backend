package org.ergoplatform.explorer.db.repositories

import cats.effect.Sync
import cats.tagless.syntax.functorK._
import derevo.derive
import doobie.ConnectionIO
import doobie.free.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.explorer.constraints.OrderingString
import org.ergoplatform.explorer.db.DoobieLogHandler
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.Token
import org.ergoplatform.explorer.{TokenId, TokenSymbol}
import tofu.higherKind.derived.representableK
import tofu.syntax.monadic._

@derive(representableK)
trait TokenRepo[D[_]] {

  def insert(token: Token): D[Unit]

  def insertMany(tokens: List[Token]): D[Unit]

  def get(id: TokenId): D[Option[Token]]

  def getBySymbol(sym: TokenSymbol): D[List[Token]]

  def getAll(offset: Int, limit: Int, ordering: OrderingString, hideNfts: Boolean): D[List[Token]]

  def countAll(hideNfts: Boolean): D[Int]

  /** Get all tokens matching a given `idSubstring`.
    */
  def getAllLike(q: String, offset: Int, limit: Int): D[List[Token]]

  /** Get the total number of tokens matching a given `idSubstring`.
    */
  def countAllLike(q: String): D[Int]
}

object TokenRepo {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[TokenRepo[D]] =
    DoobieLogHandler.create[F].map { implicit lh =>
      (new Live).mapK(LiftConnectionIO[D].liftConnectionIOK)
    }

  final class Live(implicit lh: LogHandler) extends TokenRepo[ConnectionIO] {

    import org.ergoplatform.explorer.db.queries.{TokensQuerySet => QS}

    def insert(token: Token): ConnectionIO[Unit] = QS.insertNoConflict(token).void

    def insertMany(tokens: List[Token]): ConnectionIO[Unit] = QS.insertManyNoConflict(tokens).void

    def get(id: TokenId): ConnectionIO[Option[Token]] = QS.get(id).option

    def getBySymbol(sym: TokenSymbol): ConnectionIO[List[Token]] = QS.getBySymbol(sym).to[List]

    def getAll(offset: Int, limit: Int, ordering: OrderingString, hideNfts: Boolean): ConnectionIO[List[Token]] =
      QS.getAll(offset, limit, ordering, hideNfts).to[List]

    def countAll(hideNfts: Boolean): ConnectionIO[Int] = QS.countAll(hideNfts).unique

    def getAllLike(q: String, offset: Int, limit: Int): ConnectionIO[List[Token]] =
      QS.getAllLike(q, offset, limit).to[List]

    def countAllLike(q: String): ConnectionIO[Int] =
      QS.countAllLike(q).unique
  }
}
