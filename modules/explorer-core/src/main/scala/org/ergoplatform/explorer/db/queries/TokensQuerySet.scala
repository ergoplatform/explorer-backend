package org.ergoplatform.explorer.db.queries

import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.fragment.Fragment
import doobie.util.log.LogHandler
import doobie.util.query.Query0
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.constraints.OrderingString
import org.ergoplatform.explorer.db.models.Token

object TokensQuerySet extends QuerySet {

  val tableName: String = "tokens"

  val fields: List[String] = List(
    "token_id",
    "box_id",
    "emission_amount",
    "name",
    "description",
    "type",
    "decimals"
  )

  def get(id: TokenId): Query0[Token] =
    sql"""
         |select t.token_id, t.box_id, t.emission_amount, t.name, t.description, t.type, t.decimals from tokens t
         |left join node_outputs o on o.box_id = t.box_id
         |where t.token_id = $id and o.main_chain = true
         |""".stripMargin.query[Token]

  def getAll(offset: Int, limit: Int, ordering: OrderingString)(implicit lh: LogHandler): Query0[Token] = {
    val query =
      sql"""
         |select t.token_id, t.box_id, t.emission_amount, t.name, t.description, t.type, t.decimals from tokens t
         |left join node_outputs o on o.box_id = t.box_id
         |where o.main_chain = true
         |""".stripMargin
    val orderingFr    = Fragment.const(s"order by o.creation_height $ordering")
    val offsetLimitFr = Fragment.const(s"offset $offset limit $limit")
    (query ++ orderingFr ++ offsetLimitFr).query[Token]
  }

  def countAll(implicit lh: LogHandler): Query0[Int] =
    sql"select count(*) from tokens t left join node_outputs o on o.box_id = t.box_id where o.main_chain = true"
      .query[Int]

  def getAllLike(idSubstring: String, offset: Int, limit: Int)(implicit lh: LogHandler): Query0[Token] =
    sql"""
         |select t.token_id, t.box_id, t.emission_amount, t.name, t.description, t.type, t.decimals from tokens t
         |left join node_outputs o on o.box_id = t.box_id
         |where t.token_id like ${idSubstring + "%"} and o.main_chain = true
         |offset $offset limit $limit
         """.stripMargin.query[Token]

  def countAllLike(idSubstring: String)(implicit lh: LogHandler): Query0[Int] =
    sql"""
         |select count(*) from tokens t
         |left join node_outputs o on t.box_id = o.box_id
         |where t.token_id like ${idSubstring + "%"} and o.main_chain = true
         """.stripMargin.query[Int]
}
