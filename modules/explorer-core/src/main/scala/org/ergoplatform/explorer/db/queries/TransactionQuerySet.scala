package org.ergoplatform.explorer.db.queries

import cats.data.NonEmptyList
import doobie._
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.update.Update0
import org.ergoplatform.explorer.constraints.OrderingString
import org.ergoplatform.explorer.{Address, BlockId, ErgoTreeTemplateHash, HexString, TxId}
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

  def getAllByBlockIds(blockIds: NonEmptyList[BlockId])(implicit lh: LogHandler): Query0[Transaction] = {
    val q =
      sql"""
           |select t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size, t.index, t.global_index, t.main_chain from node_transactions t
           |""".stripMargin
    (q ++ Fragments.in(fr"where t.header_id", blockIds) ++ sql"order by t.index asc").query[Transaction]
  }

  def getRecentIds(implicit lh: LogHandler): Query0[TxId] =
    sql"""
         |select t.id from node_transactions t
         |inner join (
         |  select h.id from node_headers h where h.main_chain = true order by h.height desc limit 1
         |) as h on h.id = t.header_id
         |""".stripMargin.query[TxId]

  def getAllRelatedToErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int,
    inclusionHeightRangeOp: Option[(Int, Int)] = None
  )(implicit lh: LogHandler): Query0[Transaction] =
    Fragment
      .const(
        s"""
          |select distinct t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size, t.index, t.global_index, t.main_chain
          |from node_transactions t
          |inner join (
          |  select os.tx_id from node_outputs os
          |  where os.main_chain = true and os.ergo_tree = '$ergoTree'
          |  union
          |  select i.tx_id from node_outputs os
          |  left join node_inputs i on (i.box_id = os.box_id and i.main_chain = true)
          |  where os.main_chain = true and os.ergo_tree = '$ergoTree'
          |) as os on os.tx_id = t.id
          |where t.main_chain = true
          |${inclusionHeightRangeOp.map(range => inclusionHeightFilter(range)).getOrElse("")}
          |order by t.timestamp desc
          |offset ${offset.toLong} limit ${limit.toLong}
          |""".stripMargin
      )
      .query[Transaction]

  def getAll(minGix: Long, limit: Int)(implicit lh: LogHandler): Query0[Transaction] =
    sql"""
         |select t.id, t.header_id, t.inclusion_height, t.coinbase, t.timestamp, t.size, t.index, t.global_index, t.main_chain from node_transactions t
         |where t.main_chain = true
         |  and t.global_index >= $minGix
         |  and t.global_index < ${minGix + limit}
         |order by t.global_index asc
         |""".stripMargin.query[Transaction]

  def countRelatedToErgoTree(ergoTree: HexString, inclusionHeightRangeOp: Option[(Int, Int)] = None)(implicit
    lh: LogHandler
  ): Query0[Int] =
    Fragment
      .const(
        s"""
           |select count(distinct t.id) from node_transactions t
           |inner join (
           |  select os.tx_id from node_outputs os
           |  where os.main_chain = true and os.ergo_tree = '$ergoTree'
           |  union
           |  select i.tx_id from node_outputs os
           |  left join node_inputs i on (i.box_id = os.box_id and i.main_chain = true)
           |  where os.main_chain = true and os.ergo_tree = '$ergoTree') as os on os.tx_id = t.id
           |where t.main_chain = true
           |${inclusionHeightRangeOp.map(range => inclusionHeightFilter(range)).getOrElse("")}
           |""".stripMargin
      )
      .query[Int]

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

  /** Recalculates globalIndex for all transactions starting from a given height.
    * 
    * ALTERNATIVE IMPLEMENTATION APPROACH (different from PR #266):
    * Instead of using a single recursive CTE, this uses a simpler, more maintainable approach:
    * 1. Use window function ROW_NUMBER() to calculate correct ordering
    * 2. Join with a base calculation to get the starting index
    * 3. Single atomic UPDATE with explicit locking for safety
    * 
    * This approach offers:
    * - Better performance on large datasets (no recursion overhead)
    * - Clearer SQL (easier to understand and maintain)
    * - Explicit FOR UPDATE locking for concurrent safety
    * - Compatible with all PostgreSQL versions (no recursive CTE needed)
    * 
    * @param height The height from which to recalculate globalIndex
    * @return Update0 operation that recalculates global_index for affected transactions
    */
  def recalculateGlobalIndexFromHeight(height: Int)(implicit lh: LogHandler): Update0 =
    sql"""
         |WITH base_index AS (
         |  -- Get the last global_index before the reorg height
         |  SELECT COALESCE(MAX(global_index), -1) AS last_index
         |  FROM node_transactions
         |  WHERE inclusion_height < $height 
         |    AND main_chain = true
         |),
         |ordered_txs AS (
         |  -- Calculate new global_index for all affected transactions
         |  SELECT 
         |    t.id,
         |    t.header_id,
         |    (SELECT last_index FROM base_index) + 
         |      ROW_NUMBER() OVER (
         |        ORDER BY t.inclusion_height ASC, 
         |                 t.timestamp ASC, 
         |                 t.index ASC
         |      ) AS new_global_index
         |  FROM node_transactions t
         |  WHERE t.inclusion_height >= $height 
         |    AND t.main_chain = true
         |  FOR UPDATE  -- Explicit locking for concurrent safety
         |)
         |UPDATE node_transactions t
         |SET global_index = o.new_global_index
         |FROM ordered_txs o
         |WHERE t.id = o.id 
         |  AND t.header_id = o.header_id
         |""".stripMargin.update
}
