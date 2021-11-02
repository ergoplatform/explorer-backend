package org.ergoplatform.explorer.db.queries

import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.fragment.Fragment
import doobie.util.log.LogHandler
import doobie.util.query.Query0
import org.ergoplatform.explorer.{TokenId, TokenSymbol}
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

  def getBySymbol(sym: TokenSymbol): Query0[Token] =
    sql"""
         |select t.token_id, t.box_id, t.emission_amount, t.name, t.description, t.type, t.decimals from tokens t
         |left join node_outputs o on o.box_id = t.box_id
         |where t.name = $sym and o.main_chain = true
         |""".stripMargin.query[Token]

  def getAll(offset: Int, limit: Int, ordering: OrderingString, hideNfts: Boolean)(implicit
    lh: LogHandler
  ): Query0[Token] = {
    val q =
      sql"""
         |select t.token_id, t.box_id, t.emission_amount, t.name, t.description, t.type, t.decimals from tokens t
         |left join node_outputs o on o.box_id = t.box_id
         |where o.main_chain = true
         |""".stripMargin
    val nonNft        = Fragment.const(s" and t.emission_amount > 1")
    val orderingFr    = Fragment.const(s"order by o.creation_height $ordering")
    val offsetLimitFr = Fragment.const(s"offset $offset limit $limit")
    ((if (hideNfts) q ++ nonNft else q) ++ orderingFr ++ offsetLimitFr).query[Token]
  }

  def countAll(hideNfts: Boolean)(implicit lh: LogHandler): Query0[Int] = {
    val q      = sql"select count(*) from tokens t left join node_outputs o on o.box_id = t.box_id where o.main_chain = true"
    val nonNft = Fragment.const(s" and t.emission_amount > 1")
    (if (hideNfts) q ++ nonNft else q).query[Int]
  }

  def getAllLike(q: String, offset: Int, limit: Int)(implicit lh: LogHandler): Query0[Token] =
    sql"""
         |select t.token_id, t.box_id, t.emission_amount, t.name, t.description, t.type, t.decimals from tokens t
         |left join node_outputs o on o.box_id = t.box_id
         |where (t.token_id like ${q + "%"} or t.name like ${q + "%"}) and o.main_chain = true
         |offset $offset limit $limit
         """.stripMargin.query[Token]

  def countAllLike(q: String)(implicit lh: LogHandler): Query0[Int] =
    sql"""
         |select count(*) from tokens t
         |left join node_outputs o on t.box_id = o.box_id
         |where (t.token_id like ${q + "%"} or t.name like ${q + "%"}) and o.main_chain = true
         """.stripMargin.query[Int]
}
