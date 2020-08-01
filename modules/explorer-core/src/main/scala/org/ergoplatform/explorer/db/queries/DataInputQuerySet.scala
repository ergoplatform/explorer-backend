package org.ergoplatform.explorer.db.queries

import cats.data.NonEmptyList
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.query.Query0
import doobie.util.update.Update0
import doobie.{Fragments, LogHandler}
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedDataInput, ExtendedInput}
import org.ergoplatform.explorer.{Id, TxId}

/** A set of queries for doobie implementation of [InputRepo].
  */
object DataInputQuerySet extends QuerySet {

  import org.ergoplatform.explorer.db.doobieInstances._

  val tableName: String = "node_data_inputs"

  val fields: List[String] = List(
    "box_id",
    "tx_id",
    "header_id",
    "index",
    "main_chain"
  )

  def getAllByTxId(txId: TxId)(implicit lh: LogHandler): Query0[ExtendedDataInput] =
    sql"""
         |select distinct on (i.box_id)
         |  i.box_id,
         |  i.tx_id,
         |  i.header_id,
         |  i.index,
         |  i.main_chain,
         |  o.value,
         |  o.tx_id,
         |  o.address
         |from node_data_inputs i
         |join node_outputs o on i.box_id = o.box_id
         |where i.tx_id = $txId
         |""".stripMargin.query[ExtendedDataInput]

  def getAllByTxIds(txsId: NonEmptyList[TxId])(implicit lh: LogHandler): Query0[ExtendedDataInput] = {
    val q =
      sql"""
           |select distinct on (i.box_id)
           |  i.box_id,
           |  i.tx_id,
           |  i.header_id,
           |  i.index,
           |  i.main_chain,
           |  o.value,
           |  o.tx_id,
           |  o.address
           |from node_data_inputs i
           |join node_outputs o on i.box_id = o.box_id
           |""".stripMargin
    (q ++ Fragments.in(fr"where i.tx_id", txsId))
      .query[ExtendedDataInput]
  }

  def updateChainStatusByHeaderId(headerId: Id, newChainStatus: Boolean)(implicit lh: LogHandler): Update0 =
    sql"""
         |update node_data_inputs set main_chain = $newChainStatus
         |where header_id = $headerId
         """.stripMargin.update
}
