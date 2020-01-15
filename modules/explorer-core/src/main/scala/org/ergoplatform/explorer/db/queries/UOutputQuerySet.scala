package org.ergoplatform.explorer.db.queries

import fs2.Stream
import doobie._
import doobie.implicits._
import doobie.refined.implicits._
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.db.models.UOutput

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
    "address",
    "additional_registers"
  )

  def getAll(offset: Int, limit: Int): Stream[ConnectionIO, UOutput] =
    sql"select * from node_u_outputs offset $offset limit $limit".query[UOutput].stream

  def getAllByTxId(txId: TxId): ConnectionIO[List[UOutput]] =
    sql"select * from node_u_outputs where tx_id = $txId".query[UOutput].to[List]
}
