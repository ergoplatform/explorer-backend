package org.ergoplatform.explorer.db.queries

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.refined.implicits._
import org.ergoplatform.explorer.{Address, Id, TxId}
import org.ergoplatform.explorer.db.models.Transaction
import fs2.Stream

/** A set of queries for doobie implementation of [TransactionRepo].
  */
object TransactionQuerySet extends QuerySet {

  val tableName: String = "node_transactions"

  val fields: List[String] = List(
    "id",
    "header_id",
    "coinbase",
    "timestamp",
    "size"
  )

  def getMain(id: TxId): ConnectionIO[Option[Transaction]] =
    sql"""
         |select t.id, t.header_id, t.coinbase, t.timestamp, t.size from node_transactions t
         |left join node_headers h on h.id = t.header_id
         |where h.main_chain = true and t.id = $id
         |""".stripMargin.query[Transaction].option

  def getAllMainByIdSubstring(idStr: String): ConnectionIO[List[Transaction]] =
    sql"""
         |select t.id, t.header_id, t.coinbase, t.timestamp, t.size from node_transactions t
         |left join node_headers h on h.id = t.header_id
         |where t.id like ${s"%$idStr%"} and h.main_chain = true
         |""".stripMargin.query[Transaction].to[List]

  def getAllByBlockId(id: Id): Stream[ConnectionIO, Transaction] =
    sql"""
         |select t.id, t.header_id, t.coinbase, t.timestamp, t.size from node_transactions t
         |where header_id = $id
         |""".stripMargin.query[Transaction].stream

  def getAllRelatedToAddress(
    address: Address,
    offset: Int,
    limit: Int
  ): Stream[ConnectionIO, Transaction] =
    sql"""
         |select distinct t.id, t.header_id, t.coinbase, t.timestamp, t.size
         |from node_outputs os
         |left join node_inputs inp on inp.box_id = os.box_id
         |left join node_transactions t on (os.tx_id = t.id or inp.tx_id = t.id)
         |left join node_headers h on  h.id = t.header_id
         |where os.address = $address and h.main_chain = true
         |order by t.timestamp desc
         |offset ${offset.toLong} limit ${limit.toLong}
         |""".stripMargin.query[Transaction].stream

  def getAllMainSince(
    height: Int,
    offset: Int,
    limit: Int
  ): Stream[ConnectionIO, Transaction] =
    sql"""
         |select t.id, t.header_id, t.coinbase, t.timestamp, t.size from node_transactions t
         |left join node_headers h on h.id = t.header_id
         |where h.height >= $height and h.main_chain = true
         |order by t.timestamp desc
         |offset $offset limit $limit
         |""".stripMargin.query[Transaction].stream
}
