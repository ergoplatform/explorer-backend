package org.ergoplatform.explorer.db.queries

import doobie.free.connection.ConnectionIO
import org.ergoplatform.explorer.db.models.composite.ExtendedOutput
import doobie.implicits._
import doobie.refined.implicits._
import fs2.Stream
import org.ergoplatform.explorer.{Address, BoxId, HexString, Id}

/** A set of queries required to implement functionality of production [OutputRepo].
  */
object OutputQuerySet extends QuerySet {

  import org.ergoplatform.explorer.db.doobieInstances._

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

  def getByBoxId(boxId: BoxId): ConnectionIO[Option[ExtendedOutput]] =
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
         |""".stripMargin.query[ExtendedOutput].option

  def getAllByAddress(address: Address): Stream[ConnectionIO, ExtendedOutput] =
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
         |where o.address = $address
         |""".stripMargin.query[ExtendedOutput].stream

  def getAllByErgoTree(
    ergoTree: HexString
  ): Stream[ConnectionIO, ExtendedOutput] =
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
         |""".stripMargin.query[ExtendedOutput].stream

  def getAllMainUnspentByAddress(
    address: Address
  ): Stream[ConnectionIO, ExtendedOutput] =
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
         |  and o.address = $address
         |""".stripMargin.query[ExtendedOutput].stream

  def getAllMainUnspentByErgoTree(
    ergoTree: HexString
  ): Stream[ConnectionIO, ExtendedOutput] =
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
         |""".stripMargin.query[ExtendedOutput].stream

  def searchAddressesBySubstring(substring: String): ConnectionIO[List[Address]] =
    sql"select address from node_outputs where address like ${"%" + substring + "%"}"
      .query[Address]
      .to[List]

  def updateChainStatusByHeaderId(
    headerId: Id
  )(newChainStatus: Boolean): ConnectionIO[Int] =
    sql"""
         |update node_outputs set main_chain = $newChainStatus from node_outputs o
         |left join node_transactions t on t.id = o.tx_id
         |left join node_headers h on t.header_id = h.id
         |where h.id = $headerId
         |""".stripMargin.update.run
}
