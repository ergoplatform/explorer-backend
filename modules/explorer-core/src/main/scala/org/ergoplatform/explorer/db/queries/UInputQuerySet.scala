package org.ergoplatform.explorer.db.queries

import cats.data.NonEmptyList
import doobie.implicits._
import doobie.refined.implicits._
import doobie.Fragments.in
import doobie.util.query.Query0
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.db.models.UInput

object UInputQuerySet extends QuerySet {

  import org.ergoplatform.explorer.db.doobieInstances._

  val tableName: String = "node_u_inputs"

  val fields: List[String] = List(
    "box_id",
    "tx_id",
    "proof_bytes",
    "extension"
  )

  def getAll(offset: Int, limit: Int): Query0[UInput] =
    sql"select * from node_u_inputs offset $offset limit $limit".query[UInput]

  def getAllByTxId(txId: TxId): Query0[UInput] =
    sql"select * from node_u_inputs where tx_id = $txId".query[UInput]

  def getAllByTxIxs(txIds: NonEmptyList[TxId]): Query0[UInput] =
    in(sql"select * from node_u_inputs where tx_id", txIds).query[UInput]
}
