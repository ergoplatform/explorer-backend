package org.ergoplatform.explorer.db.queries

import cats.data.NonEmptyList
import doobie.implicits._
import doobie.refined.implicits._
import doobie.Fragments.in
import doobie.LogHandler
import doobie.util.query.Query0
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.db.models.UInput
import org.ergoplatform.explorer.db.models.aggregates.ExtendedUInput

object UInputQuerySet extends QuerySet {

  import org.ergoplatform.explorer.db.doobieInstances._

  val tableName: String = "node_u_inputs"

  val fields: List[String] = List(
    "box_id",
    "tx_id",
    "proof_bytes",
    "extension"
  )

  def getAll(offset: Int, limit: Int)(implicit lh: LogHandler): Query0[ExtendedUInput] =
    sql"""
         |select
         |  i.box_id,
         |  i.tx_id,
         |  i.proof_bytes,
         |  i.extension,
         |  o.value,
         |  o.tx_id,
         |  o.address
         |from node_u_inputs i
         |join node_outputs o on i.box_id = o.box_id
         |offset $offset limit $limit
         |""".query[ExtendedUInput]

  def getAllByTxId(txId: TxId)(implicit lh: LogHandler): Query0[ExtendedUInput] =
    sql"""
         |select
         |  i.box_id,
         |  i.tx_id,
         |  i.proof_bytes,
         |  i.extension,
         |  o.value,
         |  o.tx_id,
         |  o.address
         |from node_u_inputs i
         |join node_outputs o on i.box_id = o.box_id
         |where i.tx_id = $txId
         |""".query[ExtendedUInput]

  def getAllByTxIxs(txIds: NonEmptyList[TxId])(implicit lh: LogHandler): Query0[ExtendedUInput] = {
    val query =
      fr"""
         |select
         |  i.box_id,
         |  i.tx_id,
         |  i.proof_bytes,
         |  i.extension,
         |  o.value,
         |  o.tx_id,
         |  o.address
         |from node_u_inputs i
         |join node_outputs o on i.box_id = o.box_id
         |where tx_id
         |""".stripMargin
    in(query, txIds).query[ExtendedUInput]
  }
}
