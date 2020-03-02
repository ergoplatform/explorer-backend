package org.ergoplatform.explorer.db.queries

import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.query.Query0
import org.ergoplatform.explorer.{Address, Id, TxId}
import org.ergoplatform.explorer.db.models.Transaction

/** A set of queries for doobie implementation of [TransactionRepo].
  */
object TransactionQuerySet extends QuerySet {

  val tableName: String = "node_transactions"

  val fields: List[String] = List(
    "id",
    "header_id",
    "inclusion_height",
    "coinbase",
    "timestamp",
    "size"
  )

  def getMain(id: TxId): Query0[Transaction] =
    sql"""
         |select t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size from node_transactions t
         |left join node_headers h on h.id = t.header_id
         |where h.main_chain = true and t.id = $id
         |""".stripMargin.query[Transaction]

  def getAllMainByIdSubstring(idStr: String): Query0[Transaction] =
    sql"""
         |select t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size from node_transactions t
         |left join node_headers h on h.id = t.header_id
         |where t.id like ${s"%$idStr%"} and h.main_chain = true
         |""".stripMargin.query[Transaction]

  def getAllByBlockId(id: Id): Query0[Transaction] =
    sql"""
         |select t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size from node_transactions t
         |where t.header_id = $id
         |""".stripMargin.query[Transaction]

  def getAllRelatedToAddress(
    address: Address,
    offset: Int,
    limit: Int
  ): Query0[Transaction] =
    sql"""
         |select distinct t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size
         |from node_outputs os
         |left join node_inputs inp on inp.box_id = os.box_id
         |left join node_transactions t on (os.tx_id = t.id or inp.tx_id = t.id)
         |left join node_headers h on h.id = t.header_id
         |where os.address = $address and h.main_chain = true
         |order by t.timestamp desc
         |offset ${offset.toLong} limit ${limit.toLong}
         |""".stripMargin.query[Transaction]

  def countRelatedToAddress(address: Address): Query0[Int] =
    sql"""
         |select count(distinct t.id)
         |from node_outputs os
         |left join node_inputs inp on inp.box_id = os.box_id
         |left join node_transactions t on (os.tx_id = t.id or inp.tx_id = t.id)
         |left join node_headers h on h.id = t.header_id
         |where os.address = $address and h.main_chain = true
         |""".stripMargin.query[Int]

  def countMainSince(ts: Long): Query0[Int] =
    sql"select count(id) from node_transactions where timestamp >= $ts".query[Int]

  def getAllMainSince(
    height: Int,
    offset: Int,
    limit: Int
  ): Query0[Transaction] =
    sql"""
         |select t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size from node_transactions t
         |left join node_headers h on h.id = t.header_id
         |where h.height >= $height and h.main_chain = true
         |order by t.timestamp desc
         |offset $offset limit $limit
         |""".stripMargin.query[Transaction]
}
