package org.ergoplatform.explorer.db.queries

import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.log.LogHandler
import doobie.util.query.Query0
import org.ergoplatform.explorer.constraints.OrderingString
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

  def getAll(offset: Int, limit: Int, ordering: OrderingString)(implicit lh: LogHandler): Query0[Token] =
    sql"""
         |select t.token_id, t.box_id, t.name, t.description, t.type, t.decimals, t.emission_amount from tokens t
         |left join node_outputs o on o.box_id = t.box_id
         |order by o.creation_height $ordering
         |offset $offset limit $limit
         |""".stripMargin.query[Token]

  def countAll(implicit lh: LogHandler): Query0[Int] =
    sql"select count(*) from tokens".query[Int]
}
