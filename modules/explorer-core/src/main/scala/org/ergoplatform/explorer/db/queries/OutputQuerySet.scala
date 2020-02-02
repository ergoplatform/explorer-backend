package org.ergoplatform.explorer.db.queries

import java.sql.Types

import cats.data.NonEmptyList
import doobie.Fragments
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.query.Query0
import doobie.util.update.Update0
import io.circe.Json
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer._

/** A set of queries for doobie implementation of [OutputRepo].
  */
object OutputQuerySet extends QuerySet {

  import org.ergoplatform.explorer.db.doobieInstances._
  import org.ergoplatform.explorer.db.models.schema
  import org.ergoplatform.explorer.db.models.schema.ctx._

  val tableName: String = "node_outputs"

  val fields: List[String] = List(
    "box_id",
    "tx_id",
    "value",
    "creation_height",
    "index",
    "ergo_tree",
    "address",
    "additional_registers",
    "timestamp",
    "main_chain"
  )

  def getByBoxId(boxId: BoxId): Query0[ExtendedOutput] =
    sql"""
         |select
         |  o.box_id,
         |  o.tx_id,
         |  o.value,
         |  o.creation_height,
         |  o.index,
         |  o.ergo_tree,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain,
         |  i.tx_id
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id
         |where o.box_id = $boxId
         |""".stripMargin.query[ExtendedOutput]

  def getByErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int
  ): Query0[ExtendedOutput] =
    sql"""
         |select
         |  o.box_id,
         |  o.tx_id,
         |  o.value,
         |  o.creation_height,
         |  o.index,
         |  o.ergo_tree,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain,
         |  i.tx_id
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id
         |where o.ergo_tree = $ergoTree
         |""".stripMargin.query[ExtendedOutput]

  def getMainUnspentByErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int
  ): Query0[ExtendedOutput] =
    sql"""
         |select
         |  o.box_id,
         |  o.tx_id,
         |  o.value,
         |  o.creation_height,
         |  o.index,
         |  o.ergo_tree,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain,
         |  i.tx_id
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id
         |where o.main_chain = true
         |  and (i.box_id is null or i.main_chain = false)
         |  and o.ergo_tree = $ergoTree
         |offset $offset limit $limit
         |""".stripMargin.query[ExtendedOutput]

  def getMainUnspentByErgoTreeTemplate(
    ergoTreeTemplate: HexString,
    offset: Int,
    limit: Int
  ) =
    quote {
      schema.Outputs
        .leftJoin(schema.Inputs)
        .on((o, i) => i.boxId == o.boxId)
        .filter(_._1.mainChain == true)
        .filter {
          case (o, _) =>
            infix"${o.ergoTree} like %${lift(ergoTreeTemplate.unwrapped)}"
              .as[Boolean]
        }
        .filter {
          case (_, i) => i.map(_.boxId).isEmpty || !i.getOrNull.mainChain
        }
        .drop(lift(offset))
        .take(lift(limit))
        .map { case (o, i) => ExtendedOutput(o, i.map(_.txId)) }
    }

  def getAllByTxId(txId: TxId): Query0[ExtendedOutput] =
    sql"""
         |select distinct on (i.box_id)
         |  o.box_id,
         |  o.tx_id,
         |  o.value,
         |  o.creation_height,
         |  o.index,
         |  o.ergo_tree,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain,
         |  i.tx_id
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id
         |where o.tx_id = $txId
         |""".stripMargin.query[ExtendedOutput]

  def getAllByTxIds(
    txIds: NonEmptyList[TxId]
  ): Query0[ExtendedOutput] = {
    val q =
      sql"""
           |select distinct on (i.box_id)
           |  o.box_id,
           |  o.tx_id,
           |  o.value,
           |  o.creation_height,
           |  o.index,
           |  o.ergo_tree,
           |  o.address,
           |  o.additional_registers,
           |  o.timestamp,
           |  o.main_chain,
           |  i.tx_id
           |from node_outputs o
           |left join node_inputs i on o.box_id = i.box_id
           |""".stripMargin
    (q ++ Fragments.in(fr"where o.tx_id", txIds))
      .query[ExtendedOutput]
  }

  def searchAddressesBySubstring(substring: String): Query0[Address] =
    sql"select address from node_outputs where address like ${"%" + substring + "%"}"
      .query[Address]

  def updateChainStatusByHeaderId(headerId: Id)(newChainStatus: Boolean): Update0 =
    sql"""
         |update node_outputs set main_chain = $newChainStatus from node_outputs o
         |left join node_transactions t on t.id = o.tx_id
         |left join node_headers h on t.header_id = h.id
         |where h.id = $headerId
         |""".stripMargin.update
}
