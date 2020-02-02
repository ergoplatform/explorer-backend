package org.ergoplatform.explorer.db.queries

import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.query.Query0
import org.ergoplatform.explorer.db.models.UTransaction
import org.ergoplatform.explorer.{HexString, TxId}

object UTransactionQuerySet extends QuerySet {

  val tableName: String = "node_u_transactions"

  val fields: List[String] = List(
    "id",
    "creation_timestamp",
    "size"
  )

  def get(id: TxId): Query0[UTransaction] =
    sql"select * from node_u_transactions where id = $id".query[UTransaction]

  def getAllRelatedToErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int
  ): Query0[UTransaction] =
    sql"""
         |select t.id, t.creation_timestamp, t.size from node_u_transactions t
         |left join node_u_inputs ui on ui.tx_id = t.id
         |left join node_u_outputs uo on uo.tx_id = t.id
         |left join node_outputs o on o.box_id = ui.box_id
         |where uo.ergo_tree = $ergoTree or o.ergo_tree = $ergoTree
         |""".stripMargin.query[UTransaction]
}
