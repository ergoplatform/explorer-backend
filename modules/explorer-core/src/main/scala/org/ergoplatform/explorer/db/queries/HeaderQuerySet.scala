package org.ergoplatform.explorer.db.queries

import doobie.LogHandler
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.query.Query0
import doobie.util.update.Update0
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.db.models.Header

/** A set of queries for doobie implementation of [HeaderRepo].
  */
object HeaderQuerySet extends QuerySet {

  val tableName: String = "node_headers"

  val fields: List[String] = List(
    "id",
    "parent_id",
    "version",
    "height",
    "n_bits",
    "difficulty",
    "timestamp",
    "state_root",
    "ad_proofs_root",
    "transactions_root",
    "extension_hash",
    "miner_pk",
    "w",
    "n",
    "d",
    "votes",
    "main_chain"
  )

  def get(id: Id)(implicit lh: LogHandler): Query0[Header] =
    sql"select * from node_headers where id = $id".query[Header]

  def getLast(implicit lh: LogHandler): Query0[Header] =
    sql"select * from node_headers where main_chain = true order by height desc limit 1".query[Header]

  def getByParentId(parentId: Id)(implicit lh: LogHandler): Query0[Header] =
    sql"select * from node_headers where parent_id = $parentId".query[Header]

  def getAllByHeight(height: Int)(implicit lh: LogHandler): Query0[Header] =
    sql"select * from node_headers where height = $height order by main_chain desc".query[Header]

  def getHeightOf(id: Id)(implicit lh: LogHandler): Query0[Int] =
    sql"select height from node_headers where id = $id".query[Int]

  def updateChainStatusById(id: Id, newChainStatus: Boolean)(implicit lh: LogHandler): Update0 =
    sql"""
         |update node_headers set main_chain = $newChainStatus
         |where id = $id
         |""".stripMargin.update

  def getBestHeight(implicit lh: LogHandler): Query0[Int] =
    sql"select max(height) from node_headers where main_chain = true"
      .query[Int]
}
