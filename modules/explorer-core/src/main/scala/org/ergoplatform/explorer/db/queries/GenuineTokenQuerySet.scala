package org.ergoplatform.explorer.db.queries

import doobie.implicits._
import doobie.util.fragment.Fragment
import doobie.util.log.LogHandler
import doobie.util.query.Query0
import org.ergoplatform.explorer.{TokenId, TokenName}
import org.ergoplatform.explorer.db.models.GenuineToken

object GenuineTokenQuerySet extends QuerySet {

  /** Name of the table according to a database schema.
    */
  override val tableName: String = "genuine_tokens"

  /** Table column names listing according to a database schema.
    */
  override val fields: List[String] = List(
    "token_id",
    "token_name",
    "unique_name",
    "issuer"
  )

  def get(id: TokenId): Query0[GenuineToken] =
    sql"""
         |select gt.token_id, gt.token_name, gt.unique_name, gt.issuer from genuine_tokens gt
         |where gt.token_id = $id
         |""".stripMargin.query[GenuineToken]

  def get(id: TokenId, name: String): Query0[GenuineToken] =
    sql"""
         |select gt.token_id, gt.token_name, gt.unique_name, gt.issuer from genuine_tokens gt
         |where gt.token_id = $id
         | and LOWER(gt.token_name) = LOWER($name)
         |""".stripMargin.query[GenuineToken]

  def get(name: String): Query0[GenuineToken] =
    sql"""
         |select gt.token_id, gt.token_name, gt.unique_name, gt.issuer from genuine_tokens gt
         |where LOWER(gt.token_name) = LOWER($name)
         |""".stripMargin.query[GenuineToken]

  def get(name: TokenName, unique: Boolean): Query0[GenuineToken] =
    sql"""
         |select gt.token_id, gt.token_name, gt.unique_name, gt.issuer from genuine_tokens gt
         |where LOWER(gt.token_name) = LOWER($name)
         | and gt.unique_name = $unique
         |""".stripMargin.query[GenuineToken]

  def getAll(offset: Int, limit: Int)(implicit
    lh: LogHandler
  ): Query0[GenuineToken] = {
    val q =
      sql"""
           |select gt.token_id, gt.token_name, gt.unique_name, gt.issuer from genuine_tokens gt
           |""".stripMargin
    val offsetLimitFr = Fragment.const(s"offset $offset limit $limit")
    (q ++ offsetLimitFr).query[GenuineToken]
  }

  def countAll()(implicit lh: LogHandler): Query0[Int] =
    sql"select count(*) from genuine_tokens"
      .query[Int]
}
