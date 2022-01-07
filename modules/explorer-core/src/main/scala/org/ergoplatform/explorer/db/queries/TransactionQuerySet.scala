package org.ergoplatform.explorer.db.queries

import doobie.LogHandler
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.fragment.Fragment
import doobie.util.query.Query0
import doobie.util.update.Update0
import org.ergoplatform.explorer.constraints.OrderingString
import org.ergoplatform.explorer.{Address, ErgoTreeTemplateHash, BlockId, TxId}
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
    "global_index",
    "main_chain"
  )

  def getMain(id: TxId)(implicit lh: LogHandler): Query0[Transaction] =
    sql"""
         |select t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size, t.index, t.global_index, t.main_chain from node_transactions t
         |left join node_headers h on h.id = t.header_id
         |where h.main_chain = true and t.id = $id
         |""".stripMargin.query[Transaction]

  def getAllMainByIdSubstring(idSubstr: String)(implicit lh: LogHandler): Query0[Transaction] =
    sql"""
         |select t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size, t.index, t.global_index, t.main_chain from node_transactions t
         |left join node_headers h on h.id = t.header_id
         |where t.id like ${s"%$idSubstr%"} and h.main_chain = true
         |""".stripMargin.query[Transaction]

  def getAllByBlockId(id: BlockId)(implicit lh: LogHandler): Query0[Transaction] =
    sql"""
         |select t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size, t.index, t.global_index, t.main_chain from node_transactions t
         |where t.header_id = $id
         |order by t.index asc
         |""".stripMargin.query[Transaction]

  def getRecentIds(implicit lh: LogHandler): Query0[TxId] =
    sql"""
         |select t.id from node_transactions t
         |inner join (
         |  select h.id from node_headers h where h.main_chain = true order by h.height desc limit 1
         |) as h on h.id = t.header_id
         |""".stripMargin.query[TxId]

  def getAllRelatedToAddress(
    address: Address,
    offset: Int,
    limit: Int
  )(implicit lh: LogHandler): Query0[Transaction] =
    sql"""
         |select distinct t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size, t.index, t.global_index, t.main_chain
         |from node_transactions t
         |inner join (
         |  select os.tx_id from node_outputs os
         |  where os.main_chain = true and os.address = $address
         |  union
         |  select i.tx_id from node_outputs os
         |  left join node_inputs i on (i.box_id = os.box_id and i.main_chain = true)
         |  where os.main_chain = true and os.address = $address
         |) as os on os.tx_id = t.id
         |where t.main_chain = true
         |order by t.timestamp desc
         |offset ${offset.toLong} limit ${limit.toLong}
         |""".stripMargin.query[Transaction]

  def getAll(minGix: Long, limit: Int)(implicit lh: LogHandler): Query0[Transaction] =
    sql"""
         |select t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size, t.index, t.global_index, t.main_chain from node_transactions t
         |where t.main_chain = true
         |  and t.global_index >= $minGix
         |  and t.global_index < ${minGix + limit}
         |order by t.global_index asc
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
         |where t.main_chain = true
         |""".stripMargin.query[Int]

  def countMainSince(ts: Long): Query0[Int] =
    sql"select count(id) from node_transactions where timestamp >= $ts".query[Int]

  def getAllMainSince(
    height: Int,
    offset: Int,
    limit: Int
  )(implicit lh: LogHandler): Query0[Transaction] =
    sql"""
         |select t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size, t.index, t.global_index, t.main_chain from node_transactions t
         |left join node_headers h on h.id = t.header_id
         |where h.height >= $height and h.main_chain = true
         |order by t.timestamp desc
         |offset $offset limit $limit
         |""".stripMargin.query[Transaction]

  def getIdsLike(q: String)(implicit lh: LogHandler): Query0[TxId] =
    sql"select distinct id from node_transactions where id like ${s"%$q%"}".query[TxId]

  def getByInputsScriptTemplate(template: ErgoTreeTemplateHash, offset: Int, limit: Int, ordering: OrderingString)(
    implicit lh: LogHandler
  ): Query0[Transaction] = {
    val query =
      sql"""
         |select distinct on (t.id, t.inclusion_height)
         |  t.id,
         |  t.header_id,
         |  t.inclusion_height,
         |  t.coinbase,
         |  t.timestamp,
         |  t.size,
         |  t.index,
         |  t.global_index,
         |  t.main_chain
         |from node_transactions t
         |inner join node_inputs i on i.tx_id = t.id and i.header_id = t.header_id
         |inner join node_outputs o on o.box_id = i.box_id and i.header_id = t.header_id
         |where o.ergo_tree_template_hash = $template and t.main_chain = true
         |""".stripMargin
    val orderingFr    = Fragment.const(s"order by t.inclusion_height $ordering")
    val offsetLimitFr = Fragment.const(s"offset $offset limit $limit")
    (query ++ orderingFr ++ offsetLimitFr).query[Transaction]
  }

  def countByInputsScriptTemplate(template: ErgoTreeTemplateHash)(implicit
    lh: LogHandler
  ): Query0[Int] =
    sql"""
         |select count(distinct t.id)
         |from node_transactions t
         |inner join node_inputs i on i.tx_id = t.id and i.header_id = t.header_id
         |inner join node_outputs o on o.box_id = i.box_id and i.header_id = t.header_id
         |where o.ergo_tree_template_hash = $template and t.main_chain = true
         |""".stripMargin.query[Int]

  def updateChainStatusByHeaderId(headerId: BlockId, newChainStatus: Boolean)(implicit lh: LogHandler): Update0 =
    sql"""
         |update node_transactions set main_chain = $newChainStatus
         |where header_id = $headerId
         |""".stripMargin.update
}
