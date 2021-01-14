package org.ergoplatform.explorer.db.queries

import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.fragment.Fragment
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

  def getAll(offset: Int, limit: Int, ordering: OrderingString)(implicit lh: LogHandler): Query0[Token] = {
    val query =
      sql"""
         |select t.token_id, t.box_id, t.name, t.description, t.type, t.decimals, t.emission_amount from tokens t
         |left join node_outputs o on o.box_id = t.box_id
         |where o.main_chain = true
         |""".stripMargin
    val orderingFr    = Fragment.const(s"order by o.creation_height $ordering")
    val offsetLimitFr = Fragment.const(s"offset $offset limit $limit")
    (query ++ orderingFr ++ offsetLimitFr).query[Token]
  }

  def countAll(implicit lh: LogHandler): Query0[Int] =
    sql"select count(*) from tokens t left join node_outputs o on o.box_id = t.box_id where o.main_chain = true".query[Int]
}
