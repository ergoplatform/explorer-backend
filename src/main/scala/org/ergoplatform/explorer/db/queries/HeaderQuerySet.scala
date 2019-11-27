package org.ergoplatform.explorer.db.queries

import doobie.ConnectionIO
import doobie.implicits._
import doobie.refined.implicits._
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.db.models.Header

/** A set of queries required to implement functionality of production [HeaderRepo].
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

  def get(id: Id): ConnectionIO[Option[Header]] =
    sql"select * from node_headers where id = $id".query[Header].option

  def getAllByHeight(height: Int): ConnectionIO[List[Header]] =
    sql"select * from node_headers where height = $height".query[Header].to[List]

  def getHeightOf(id: Id): ConnectionIO[Option[Int]] =
    sql"select height from node_headers where id = $id".query[Int].option

  def updateChainStatusById(id: Id, newChainStatus: Boolean): ConnectionIO[Int] =
    sql"""
         |update node_headers set main_chain = $newChainStatus from node_headers h
         |where h.id = $id
         |""".stripMargin.update.run

  def getBestHeight: ConnectionIO[Option[Int]] =
    sql"SELECT height FROM blocks_info ORDER BY height DESC LIMIT 1"
      .query[Int]
      .option
}
