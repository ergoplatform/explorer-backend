package org.ergoplatform.explorer.db.queries

import cats.data.NonEmptyList
import doobie.Fragments
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.refined.implicits._
import org.ergoplatform.explorer.{Id, TxId}
import org.ergoplatform.explorer.db.models.composite.ExtendedInput

/** A set of queries required to implement functionality of production [InputRepo].
  */
object InputQuerySet extends QuerySet {

  import org.ergoplatform.explorer.db.doobieInstances._

  val tableName: String = "node_inputs"

  val fields: List[String] = List(
    "box_id",
    "tx_id",
    "proof_bytes",
    "extension",
    "main_chain"
  )

  def getAllByTxId(txId: TxId): ConnectionIO[List[ExtendedInput]] =
    sql"""
         |select distinct on (i.box_id)
         |  i.box_id,
         |  i.tx_id,
         |  i.proof_bytes,
         |  i.extension,
         |  i.main_chain,
         |  o.value,
         |  o.tx_id,
         |  o.address
         |from node_inputs i
         |join node_outputs o on i.box_id = o.box_id
         |where i.tx_id = $txId
         |""".stripMargin.query[ExtendedInput].to[List]

  def getAllByTxIds(
    txsId: NonEmptyList[TxId]
  ): ConnectionIO[List[ExtendedInput]] = {
    val q =
      sql"""
           |select distinct on (i.box_id)
           |  i.box_id,
           |  i.tx_id,
           |  i.proof_bytes,
           |  i.extension,
           |  i.main_chain,
           |  o.value,
           |  o.tx_id,
           |  o.address
           |from node_inputs i
           |join node_outputs o on i.box_id = o.box_id
           |where
           |""".stripMargin
    (q ++ Fragments.in(fr"i.tx_id", txsId))
      .query[ExtendedInput]
      .to[List]
  }

  def updateChainStatusByHeaderId(
    headerId: Id
  )(newChainStatus: Boolean): ConnectionIO[Int] =
    sql"""
         |update node_inputs set main_chain = $newChainStatus from node_inputs i
         |left join node_transactions t on t.id = i.tx_id
         |left join node_headers h on t.header_id = h.id
         |where h.id = $headerId
         |""".stripMargin.update.run
}
