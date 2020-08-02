package org.ergoplatform.explorer.db.queries

import cats.data.NonEmptyList
import doobie.Fragments.in
import doobie.LogHandler
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.query.Query0
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedUDataInput, ExtendedUInput}

object UDataInputQuerySet extends QuerySet {

  val tableName: String = "node_u_data_inputs"

  val fields: List[String] = List(
    "box_id",
    "tx_id"
  )

  def getAll(offset: Int, limit: Int)(implicit lh: LogHandler): Query0[ExtendedUDataInput] =
    sql"""
         |select
         |  i.box_id,
         |  i.tx_id,
         |  o.value,
         |  o.tx_id,
         |  o.address
         |from node_u_data_inputs i
         |join node_outputs o on i.box_id = o.box_id
         |offset $offset limit $limit
         |""".stripMargin.query[ExtendedUDataInput]

  def getAllByTxId(txId: TxId)(implicit lh: LogHandler): Query0[ExtendedUDataInput] =
    sql"""
         |select
         |  i.box_id,
         |  i.tx_id,
         |  o.value,
         |  o.tx_id,
         |  o.address
         |from node_u_data_inputs i
         |join node_outputs o on i.box_id = o.box_id
         |where i.tx_id = $txId
         |""".stripMargin.query[ExtendedUDataInput]

  def getAllByTxIxs(txIds: NonEmptyList[TxId])(implicit lh: LogHandler): Query0[ExtendedUDataInput] = {
    val queryFr =
      fr"""
          |select
          |  i.box_id,
          |  i.tx_id
          |  o.value,
          |  o.tx_id,
          |  o.address
          |from node_u_data_inputs i
          |join node_outputs o on i.box_id = o.box_id
          |where i.tx_id
          |""".stripMargin
    in(queryFr, txIds).query[ExtendedUDataInput]
  }
}
