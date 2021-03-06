package org.ergoplatform.explorer.db.queries

import cats.data.NonEmptyList
import doobie.Fragments.in
import doobie._
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.query.Query0
import org.ergoplatform.explorer.db.models.UOutput
import org.ergoplatform.explorer.{HexString, TxId}

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

  def getAll(offset: Int, limit: Int)(implicit lh: LogHandler): Query0[UOutput] =
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
         |  o.additional_registers
         |from node_u_outputs o
         |offset $offset limit $limit
         |""".stripMargin.query[UOutput]

  def getAllByTxId(txId: TxId)(implicit lh: LogHandler): Query0[UOutput] =
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
         |  o.additional_registers
         |from node_u_outputs o where o.tx_id = $txId
         |order by o.index asc
         |""".stripMargin.query[UOutput]

  def getAllByTxIds(txIds: NonEmptyList[TxId])(implicit lh: LogHandler): Query0[UOutput] = {
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
        |  o.additional_registers
        |from node_u_outputs o where o.tx_id
        |""".stripMargin
    in(q, txIds).query[UOutput]
  }

  def getAllByErgoTree(ergoTree: HexString)(implicit lh: LogHandler): Query0[UOutput] =
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
         |  o.additional_registers
         |from node_u_outputs o where o.ergo_tree = $ergoTree
         |""".stripMargin.query[UOutput]

  def getAllUnspentByErgoTree(ergoTree: HexString)(implicit lh: LogHandler): Query0[UOutput] =
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
         |  o.additional_registers
         |from node_u_outputs o
         |left join node_u_inputs i on i.box_id = o.box_id
         |where i.box_id is null and o.ergo_tree = $ergoTree
         |""".stripMargin.query[UOutput]

  def sumUnspentByErgoTree(
    ergoTree: HexString
  )(implicit lh: LogHandler): Query0[Long] =
    sql"""
         |select coalesce(cast(sum(o.value) as bigint), 0) from node_u_outputs o
         |left join node_u_inputs i on i.box_id = o.box_id
         |where i.box_id is null and o.ergo_tree = $ergoTree
         |""".stripMargin.query[Long]
}
