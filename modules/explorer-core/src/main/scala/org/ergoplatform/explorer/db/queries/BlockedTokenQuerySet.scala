package org.ergoplatform.explorer.db.queries

import doobie.implicits._
import doobie.util.fragment.Fragment
import doobie.util.log.LogHandler
import doobie.util.query.Query0
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.db.models.BlockedToken

object BlockedTokenQuerySet extends QuerySet {

  /** Name of the table according to a database schema.
    */
  override val tableName: String = "blocked_tokens"

  /** Table column names listing according to a database schema.
    */
  override val fields: List[String] = List(
    "token_id",
    "token_name"
  )

  def get(id: TokenId): Query0[BlockedToken] =
    sql"""
         |select gt.token_id, gt.token_name from blocked_tokens gt
         |where gt.token_id = $id
         |""".stripMargin.query[BlockedToken]

  def get(name: String): Query0[BlockedToken] =
    sql"""
         |select gt.token_id, gt.token_name from blocked_tokens gt
         |where LOWER(gt.token_name) = LOWER($name)
         |""".stripMargin.query[BlockedToken]

  def getAll(offset: Int, limit: Int)(implicit
    lh: LogHandler
  ): Query0[BlockedToken] = {
    val q =
      sql"""
           |select gt.token_id, gt.token_name from blocked_tokens gt
           |""".stripMargin
    val offsetLimitFr = Fragment.const(s"offset $offset limit $limit")
    (q ++ offsetLimitFr).query[BlockedToken]
  }

  def countAll()(implicit lh: LogHandler): Query0[Int] = {
    val q = sql"select count(*) from blocked_tokens"
    q.query[Int]
  }
}
