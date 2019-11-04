package org.ergoplatform.explorer.persistence.dao

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.fragment
import org.ergoplatform.explorer.{Id, TxId}
import org.ergoplatform.explorer.persistence.models.Transaction
import fs2.Stream

object TransactionDao extends Dao {

  val tableName: String = "node_transactions"

  val fields: List[String] = List(
    "id",
    "header_id",
    "coinbase",
    "timestamp",
    "size"
  )

  def getMain(id: TxId): ConnectionIO[Option[Transaction]] =
    (selectWithRefFr("t") ++ leftJoinHeadersOnIdFr ++ fr"where h.main_chain = true")
      .query[Transaction]
      .option

  def getAllMainByIdSubstring(idStr: String): ConnectionIO[List[Transaction]] =
    (selectWithRefFr("t") ++ leftJoinHeadersOnIdFr ++
      fr"where t.id like ${s"%$idStr%"} and h.main_chain = true")
      .query[Transaction]
      .to[List]

  def getAllByBlockId(id: Id): Stream[doobie.ConnectionIO, Transaction] =
    (selectFr ++ fr"where header_id = $id").query[Transaction].stream

  private def leftJoinHeadersOnIdFr: fragment.Fragment =
    fr"left join" ++ HeaderDao.tableNameFr ++ fr"on h.id = t.header_id"
}
