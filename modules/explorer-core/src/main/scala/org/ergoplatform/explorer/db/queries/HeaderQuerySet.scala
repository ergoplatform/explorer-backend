package org.ergoplatform.explorer.db.queries

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

  def get(id: Id): Query0[Header] =
    sql"select * from node_headers where id = $id".query[Header]

  def getLast: Query0[Header] =
    sql"select * from node_headers order by height desc limit 1".query[Header]

  def getByParentId(parentId: Id): Query0[Header] =
    sql"select * from node_headers where parent_id = $parentId".query[Header]

  def getAllByHeight(height: Int): Query0[Header] =
    sql"select * from node_headers where height = $height".query[Header]

  def getHeightOf(id: Id): Query0[Int] =
    sql"select height from node_headers where id = $id".query[Int]

  def updateChainStatusById(id: Id, newChainStatus: Boolean): Update0 =
    sql"""
         |update node_headers set main_chain = $newChainStatus from node_headers h
         |where h.id = $id
         |""".stripMargin.update

  def getBestHeight: Query0[Int] =
    sql"select height from blocks_info order by height desc limit 1"
      .query[Int]
}
