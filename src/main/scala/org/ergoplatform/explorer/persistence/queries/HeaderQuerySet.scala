package org.ergoplatform.explorer.persistence.queries

import cats.implicits._
import doobie.implicits._
import doobie.refined.implicits._
import doobie.{ConnectionIO, Update}
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.persistence.models.Header

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

  def update(h: Header): ConnectionIO[Header] =
    update.withUniqueGeneratedKeys[Header](fields: _*)(h -> h.id)

  def updateChainStatusById(id: Id)(newChainStatus: Boolean): ConnectionIO[Int] =
    sql"""
         |update h set h.main_chain = $newChainStatus from node_headers h
         |where h.id = $id
         |""".stripMargin.update.run

  private def update: Update[(Header, Id)] =
    Update[(Header, Id)](
      s"update $tableName set ${fields.map(f => s"$f = ?").mkString(", ")} where id = ?"
    )
}
