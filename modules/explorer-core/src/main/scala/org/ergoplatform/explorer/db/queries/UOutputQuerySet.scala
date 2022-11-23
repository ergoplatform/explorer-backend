package org.ergoplatform.explorer.db.queries

import cats.data.NonEmptyList
import doobie.Fragments.in
import doobie._
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.query.Query0
import org.ergoplatform.explorer.constraints.OrderingString
import org.ergoplatform.explorer.db.models.aggregates.ExtendedUOutput
import org.ergoplatform.explorer.db.models.{AnyOutput, UOutput}
import org.ergoplatform.explorer.{BoxId, HexString, TxId}

object UOutputQuerySet extends QuerySet {

  import org.ergoplatform.explorer.db.doobieInstances._

  val tableName: String = "node_u_outputs"

  val fields: List[String] = List(
    "box_id",
    "tx_id",
    "value",
    "creation_height",
    "index",
    "ergo_tree",
    "ergo_tree_template_hash",
    "address",
    "additional_registers"
  )

  def get(boxId: BoxId)(implicit lh: LogHandler): Query0[ExtendedUOutput] =
    sql"""
         |select distinct on (o.box_id)
         |  o.box_id,
         |  o.tx_id,
         |  o.value,
         |  o.creation_height,
         |  o.index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  i.tx_id
         |from node_u_outputs o
         |left join node_u_inputs i on i.box_id = o.box_id
         |where o.box_id = $boxId
         |""".stripMargin.query[ExtendedUOutput]

  def getAll(offset: Int, limit: Int)(implicit lh: LogHandler): Query0[ExtendedUOutput] =
    sql"""
         |select distinct on (o.box_id)
         |  o.box_id,
         |  o.tx_id,
         |  o.value,
         |  o.creation_height,
         |  o.index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  i.tx_id
         |from node_u_outputs o
         |left join node_u_inputs i on i.box_id = o.box_id
         |offset $offset limit $limit
         |""".stripMargin.query[ExtendedUOutput]

  def getAllUnspent(offset: Int, limit: Int)(implicit lh: LogHandler): Query0[UOutput] =
    sql"""
         |select distinct on (o.box_id)
         |  o.box_id,
         |  o.tx_id,
         |  o.value,
         |  o.creation_height,
         |  o.index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  i.tx_id
         |from node_u_outputs o
         |left join node_u_inputs i on i.box_id = o.box_id
         |where i.box_id is null
         |offset $offset limit $limit
         |""".stripMargin.query

  def getAllByTxId(txId: TxId)(implicit lh: LogHandler): Query0[ExtendedUOutput] =
    sql"""
         |select distinct on (o.box_id, o.index)
         |  o.box_id,
         |  o.tx_id,
         |  o.value,
         |  o.creation_height,
         |  o.index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  i.tx_id
         |from node_u_outputs o
         |left join node_u_inputs i on i.box_id = o.box_id
         |where o.tx_id = $txId
         |order by o.index asc
         |""".stripMargin.query[ExtendedUOutput]

  def getAllByTxIds(txIds: NonEmptyList[TxId])(implicit lh: LogHandler): Query0[ExtendedUOutput] = {
    val q =
      fr"""
        |select distinct on (o.box_id)
        |  o.box_id,
        |  o.tx_id,
        |  o.value,
        |  o.creation_height,
        |  o.index,
        |  o.ergo_tree,
        |  o.ergo_tree_template_hash,
        |  o.address,
        |  o.additional_registers,
        |  i.tx_id
        |from node_u_outputs o
        |left join node_u_inputs i on i.box_id = o.box_id
        |where o.tx_id
        |""".stripMargin
    in(q, txIds).query[ExtendedUOutput]
  }

  def getAllRelatedToErgoTree(ergoTree: HexString)(implicit
    lh: LogHandler
  ): Query0[UOutput] =
    fr"""
        |select distinct on (o.box_id)
        |  o.box_id,
        |  o.tx_id,
        |  o.value,
        |  o.creation_height,
        |  o.index,
        |  o.ergo_tree,
        |  o.ergo_tree_template_hash,
        |  o.address,
        |  o.additional_registers,
        |  i.tx_id
        |from node_u_outputs o
        |left join node_u_inputs i on i.box_id = o.box_id
        |where o.tx_id in
        | (select distinct on (t.id) t.id from node_u_transactions t
        |    left join node_u_inputs ui on ui.tx_id = t.id
        |    left join node_u_outputs uo on uo.tx_id = t.id
        |    left join node_outputs o on o.box_id = ui.box_id
        |    where uo.ergo_tree = $ergoTree or o.ergo_tree = $ergoTree)
        |""".stripMargin.query[UOutput]

  def getAllByErgoTree(ergoTree: HexString)(implicit lh: LogHandler): Query0[ExtendedUOutput] =
    sql"""
         |select distinct on (o.box_id)
         |  o.box_id,
         |  o.tx_id,
         |  o.value,
         |  o.creation_height,
         |  o.index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  i.tx_id
         |from node_u_outputs o
         |left join node_u_inputs i on i.box_id = o.box_id
         |where o.ergo_tree = $ergoTree
         |""".stripMargin.query[ExtendedUOutput]

  def getAllUnspentByErgoTree(ergoTree: HexString)(implicit lh: LogHandler): Query0[ExtendedUOutput] =
    sql"""
         |select distinct on (o.box_id)
         |  o.box_id,
         |  o.tx_id,
         |  o.value,
         |  o.creation_height,
         |  o.index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  null
         |from node_u_outputs o
         |left join node_u_inputs i on i.box_id = o.box_id
         |where i.box_id is null and o.ergo_tree = $ergoTree
         |""".stripMargin.query[ExtendedUOutput]

  def streamAllUnspentByErgoTree(ergoTree: HexString, offset: Int, limit: Int, ordering: OrderingString)(implicit
    lh: LogHandler
  ): Query0[AnyOutput] = {
    val q   = sql"""
         |select * from (
         |select distinct on (o.box_id)
         |o.box_id,
         |o.tx_id,
         |null,
         |o.value,
         |o.creation_height,
         |null,
         |o.index,
         |null,
         |o.ergo_tree,
         |o.ergo_tree_template_hash,
         |o.address,
         |o.additional_registers,
         |null,
         |null,
         |null
         |from node_u_outputs o
         |left join node_u_inputs i on i.box_id = o.box_id
         |where i.box_id is null and o.ergo_tree = $ergoTree
         |union all
         |select distinct on (o.box_id, o.global_index)
         |o.box_id,
         |o.tx_id,
         |o.header_id,
         |o.value,
         |o.creation_height,
         |o.settlement_height,
         |o.index,
         |o.global_index,
         |o.ergo_tree,
         |o.ergo_tree_template_hash,
         |o.address,
         |o.additional_registers,
         |o.timestamp,
         |o.main_chain,
         |null
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |where o.main_chain = true
         |and i.box_id is null
         |and o.ergo_tree = $ergoTree
         |) sub
         |""".stripMargin
    val ord = Fragment.const(s"order by creation_height $ordering")
    val lim = Fragment.const(s"offset $offset limit $limit")
    (q ++ ord ++ lim).query[AnyOutput]
  }

  def sumUnspentByErgoTree(
    ergoTree: HexString
  )(implicit lh: LogHandler): Query0[Long] =
    sql"""
         |select coalesce(cast(sum(o.value) as bigint), 0) from node_u_outputs o
         |left join node_u_inputs i on i.box_id = o.box_id
         |where i.box_id is null and o.ergo_tree = $ergoTree
         |""".stripMargin.query[Long]

  def countUnspentByErgoTree(
    ergoTree: HexString
  )(implicit lh: LogHandler): Query0[Int] =
    sql"""
         |select count(distinct o.box_id)
         |from node_u_outputs o
         |left join node_u_inputs i on o.box_id = i.box_id
         |where i.box_id is null
         |and o.ergo_tree = $ergoTree
         |""".stripMargin.query[Int]

  def countAllByErgoTree(
    ergoTree: HexString
  )(implicit lh: LogHandler): Query0[Int] =
    sql"""
         |SELECT sum(count) from (
         |select count(distinct o.box_id)
         |from node_u_outputs o
         |left join node_u_inputs i on o.box_id = i.box_id
         |where i.box_id is null
         |and o.ergo_tree = $ergoTree
         |union
         |select count(distinct o.box_id)
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |where o.main_chain = true
         |and i.box_id is null
         |and o.ergo_tree = $ergoTree
         |) sub
         |""".stripMargin.query[Int]
}
