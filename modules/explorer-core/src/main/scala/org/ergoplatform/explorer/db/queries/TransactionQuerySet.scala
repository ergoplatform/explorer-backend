package org.ergoplatform.explorer.db.queries

import doobie.LogHandler
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.query.Query0
import doobie.util.update.Update0
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
    "size",
    "index",
    "main_chain"
  )

  def getMain(id: TxId)(implicit lh: LogHandler): Query0[Transaction] =
    sql"""
         |select t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size, t.index, t.main_chain from node_transactions t
         |left join node_headers h on h.id = t.header_id
         |where h.main_chain = true and t.id = $id
         |""".stripMargin.query[Transaction]

  def getAllMainByIdSubstring(idSubstr: String)(implicit lh: LogHandler): Query0[Transaction] =
    sql"""
         |select t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size, t.index, t.main_chain from node_transactions t
         |left join node_headers h on h.id = t.header_id
         |where t.id like ${s"%$idSubstr%"} and h.main_chain = true
         |""".stripMargin.query[Transaction]

  def getAllByBlockId(id: Id)(implicit lh: LogHandler): Query0[Transaction] =
    sql"""
         |select t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size, t.index, t.main_chain from node_transactions t
         |where t.header_id = $id
         |order by t.index asc
         |""".stripMargin.query[Transaction]

  def getAllRelatedToAddress(
    address: Address,
    offset: Int,
    limit: Int
  )(implicit lh: LogHandler): Query0[Transaction] =
    sql"""
         |select distinct t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size, t.index, t.main_chain
         |from node_transactions t
         |inner join (
         |  select os.tx_id from node_outputs os
         |  where os.main_chain = true and os.address = $address
         |  union
         |  select i.tx_id from node_outputs os
         |  left join node_inputs i on (i.box_id = os.box_id and i.main_chain = true)
         |  where os.main_chain = true and os.address = $address
         |) as os on os.tx_id = t.id
         |order by t.timestamp desc
         |offset ${offset.toLong} limit ${limit.toLong}
         |""".stripMargin.query[Transaction]

  def countRelatedToAddress(address: Address)(implicit lh: LogHandler): Query0[Int] =
    sql"""
         |select count(distinct t.id) from node_transactions t
         |inner join (
         |  select os.tx_id from node_outputs os
         |  where os.main_chain = true and os.address = $address
         |  union
         |  select i.tx_id from node_outputs os
         |  left join node_inputs i on (i.box_id = os.box_id and i.main_chain = true)
         |  where os.main_chain = true and os.address = $address
         |) as os on os.tx_id = t.id
         |""".stripMargin.query[Int]

  def countMainSince(ts: Long): Query0[Int] =
    sql"select count(id) from node_transactions where timestamp >= $ts".query[Int]

  def getAllMainSince(
    height: Int,
    offset: Int,
    limit: Int
  )(implicit lh: LogHandler): Query0[Transaction] =
    sql"""
         |select t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size, t.index, t.main_chain from node_transactions t
         |left join node_headers h on h.id = t.header_id
         |where h.height >= $height and h.main_chain = true
         |order by t.timestamp desc
         |offset $offset limit $limit
         |""".stripMargin.query[Transaction]

  def getIdsLike(q: String)(implicit lh: LogHandler): Query0[TxId] =
    sql"select id from node_transactions where id like ${s"%$q%"}".query[TxId]

  def updateChainStatusByHeaderId(headerId: Id, newChainStatus: Boolean)(implicit lh: LogHandler): Update0 =
    sql"""
         |update node_transactions set main_chain = $newChainStatus
         |where header_id = $headerId
         |""".stripMargin.update
}
