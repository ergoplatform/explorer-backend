package org.ergoplatform.explorer.db.queries

import doobie.implicits._
import doobie.util.log.LogHandler
import doobie.util.query.Query0
import org.ergoplatform.explorer.db.models.Token

object TokensQuerySet extends QuerySet {

  val tableName: String = "tokens"

  val fields: List[String] = List(
    "token_id",
    "box_id",
    "name",
    "description",
    "type",
    "decimals",
    "emission_amount"
  )

  def getAll(offset: Int, limit: Int)(implicit lh: LogHandler): Query0[Token] =
    sql"""
         |select token_id, box_id, name, description, type, decimals, emission_amount from tokens
         |offset $offset limit $limit
         |""".query[Token]

  def countAll(implicit lh: LogHandler): Query0[Int] =
    sql"select count(*) from tokens".query[Int]
}
