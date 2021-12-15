package org.ergoplatform.explorer.db.queries

import cats.data.NonEmptyList
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.query.Query0
import doobie.Fragments.in
import doobie.LogHandler
import doobie.util.fragment.Fragment
import doobie.util.update.Update0
import org.ergoplatform.explorer.constraints.OrderingString
import org.ergoplatform.explorer.db.models.UTransaction
import org.ergoplatform.explorer.{HexString, TxId}

object UTransactionQuerySet extends QuerySet {

  val tableName: String = "node_u_transactions"

  val fields: List[String] = List(
    "id",
    "creation_timestamp",
    "size"
  )

  def dropMany(ids: NonEmptyList[TxId])(implicit lh: LogHandler): Update0 =
    in(fr"delete from node_u_transactions where id", ids).update

  def get(id: TxId)(implicit lh: LogHandler): Query0[UTransaction] =
    sql"select id, creation_timestamp, size from node_u_transactions where id = $id".query[UTransaction]

  def getAllRelatedToErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int
  )(implicit lh: LogHandler): Query0[UTransaction] =
    sql"""
         |select distinct on (t.id) t.id, t.creation_timestamp, t.size from node_u_transactions t
         |left join node_u_inputs ui on ui.tx_id = t.id
         |left join node_u_outputs uo on uo.tx_id = t.id
         |left join node_outputs o on o.box_id = ui.box_id
         |where uo.ergo_tree = $ergoTree or o.ergo_tree = $ergoTree
         |offset $offset limit $limit
         |""".stripMargin.query[UTransaction]

  def getAll(offset: Int, limit: Int)(implicit lh: LogHandler): Query0[UTransaction] =
    sql"select id, creation_timestamp, size from node_u_transactions order by creation_timestamp desc offset $offset limit $limit"
      .query[UTransaction]

  def getAll(offset: Int, limit: Int, ordering: OrderingString, orderBy: String)(implicit
    lh: LogHandler
  ): Query0[UTransaction] = {
    val ord =
      Fragment.const(s"order by $orderBy $ordering")

    val lim =
      Fragment.const(s"offset $offset limit $limit")

    val q = sql"select id, creation_timestamp, size from node_u_transactions"

    (q ++ ord ++ lim).query[UTransaction]
  }

  def getAllIds(implicit lh: LogHandler): Query0[TxId] =
    sql"select id from node_u_transactions".query[TxId]

  def countUnconfirmedTxs(implicit lh: LogHandler): Query0[Int] =
    sql"select count(*) from node_u_transactions".query[Int]

  def countByErgoTree(ergoTree: HexString)(implicit lh: LogHandler): Query0[Int] =
    sql"""
         |select count(distinct t.id) from node_u_transactions t
         |left join node_u_inputs ui on ui.tx_id = t.id
         |left join node_u_outputs uo on uo.tx_id = t.id
         |left join node_outputs o on o.box_id = ui.box_id
         |where uo.ergo_tree = $ergoTree or o.ergo_tree = $ergoTree
         |""".stripMargin.query[Int]
}
