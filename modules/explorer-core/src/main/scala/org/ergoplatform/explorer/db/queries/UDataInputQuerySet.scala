package org.ergoplatform.explorer.db.queries

import cats.data.NonEmptyList
import doobie.Fragments.in
import doobie.LogHandler
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.query.Query0
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.db.models.aggregates.ExtendedUDataInput

object UDataInputQuerySet extends QuerySet {

  val tableName: String = "node_u_data_inputs"

  val fields: List[String] = List(
    "box_id",
    "tx_id",
    "index"
  )

  def getAll(offset: Int, limit: Int)(implicit lh: LogHandler): Query0[ExtendedUDataInput] =
    sql"""
         |select
         |  i.box_id,
         |  i.tx_id,
         |  i.index,
         |  case o.value   when null then ou.value   else o.value end,
         |  case o.tx_id   when null then ou.tx_id   else o.tx_id end,
         |  case o.index   when null then ou.index   else o.index end,
         |  case o.address when null then ou.address else o.address end
         |from node_u_data_inputs i
         |left join node_outputs o on i.box_id = o.box_id
         |left join node_u_outputs ou on i.box_id = ou.box_id
         |offset $offset limit $limit
         |""".stripMargin.query[ExtendedUDataInput]

  def getAllByTxId(txId: TxId)(implicit lh: LogHandler): Query0[ExtendedUDataInput] =
    sql"""
         |select
         |  i.box_id,
         |  i.tx_id,
         |  i.index,
         |  case o.value   when null then ou.value   else o.value end,
         |  case o.tx_id   when null then ou.tx_id   else o.tx_id end,
         |  case o.index   when null then ou.index   else o.index end,
         |  case o.address when null then ou.address else o.address end
         |from node_u_data_inputs i
         |left join node_outputs o on i.box_id = o.box_id
         |left join node_u_outputs ou on i.box_id = ou.box_id
         |where i.tx_id = $txId
         |""".stripMargin.query[ExtendedUDataInput]

  def getAllByTxIxs(txIds: NonEmptyList[TxId])(implicit lh: LogHandler): Query0[ExtendedUDataInput] = {
    val queryFr =
      fr"""
          |select
          |  i.box_id,
          |  i.tx_id,
          |  i.index,
          |  case o.value   when null then ou.value   else o.value end,
          |  case o.tx_id   when null then ou.tx_id   else o.tx_id end,
          |  case o.index   when null then ou.index   else o.index end,
          |  case o.address when null then ou.address else o.address end
          |from node_u_data_inputs i
          |left join node_outputs o on i.box_id = o.box_id
          |left join node_u_outputs ou on i.box_id = ou.box_id
          |where i.tx_id
          |""".stripMargin
    in(queryFr, txIds).query[ExtendedUDataInput]
  }
}
