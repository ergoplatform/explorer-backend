package org.ergoplatform.explorer.db.queries

import cats.data.NonEmptyList
import doobie._
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.query.Query0
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer._

/** A set of queries for doobie implementation of [OutputRepo].
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

  def getByBoxId(boxId: BoxId)(implicit lh: LogHandler): Query0[ExtendedOutput] =
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
         |  (select tx.id from node_transactions tx left join node_headers h on tx.header_id = h.id where tx.id = i.tx_id and h.main_chain = true)
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id
         |where o.box_id = $boxId
         |limit 1
         |""".stripMargin.query[ExtendedOutput]

  def getMainByErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int
  )(implicit lh: LogHandler): Query0[ExtendedOutput] =
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
         |  case i.main_chain when false then null else i.tx_id end
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id
         |where o.ergo_tree = $ergoTree and o.main_chain = true
         |offset $offset limit $limit
         |""".stripMargin.query[ExtendedOutput]

  def getAllMainUnspentIdsByErgoTree(
    ergoTree: HexString
  )(implicit lh: LogHandler): Query0[BoxId] =
    sql"""
         |select o.box_id from node_outputs o
         |left join (select i.box_id, i.main_chain from node_inputs i where i.main_chain = true) as i on o.box_id = i.box_id
         |where o.main_chain = true
         |  and (i.box_id is null or i.main_chain = false)
         |  and o.ergo_tree = $ergoTree
         |""".stripMargin.query[BoxId]

  def sumOfAllMainUnspentByErgoTree(
    ergoTree: HexString
  )(implicit lh: LogHandler): Query0[Long] =
    sql"""
         |select coalesce(cast(sum(o.value) as bigint), 0) from node_outputs o
         |left join (select i.box_id, i.main_chain from node_inputs i where i.main_chain = true) as i on o.box_id = i.box_id
         |where o.main_chain = true
         |  and (i.box_id is null or i.main_chain = false)
         |  and o.ergo_tree = $ergoTree
         |""".stripMargin.query[Long]

  def getMainUnspentByErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int
  )(implicit lh: LogHandler): Query0[ExtendedOutput] =
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
         |  null
         |from node_outputs o
         |left join (select i.box_id, i.main_chain from node_inputs i where i.main_chain = true) as i on o.box_id = i.box_id
         |where o.main_chain = true
         |  and (i.box_id is null or i.main_chain = false)
         |  and o.ergo_tree = $ergoTree
         |offset $offset limit $limit
         |""".stripMargin.query[ExtendedOutput]

  def getMainUnspentByErgoTreeTemplate(
    ergoTreeTemplate: HexString,
    offset: Int,
    limit: Int
  )(implicit lh: LogHandler): Query0[ExtendedOutput] =
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
         |  null
         |from node_outputs o
         |left join (select i.box_id, i.main_chain from node_inputs i where i.main_chain = true) as i on o.box_id = i.box_id
         |where o.main_chain = true
         |  and (i.box_id is null or i.main_chain = false)
         |  and o.ergo_tree like ${"%" + ergoTreeTemplate}
         |offset $offset limit $limit
         |""".stripMargin.query[ExtendedOutput]

  def getAllByTxId(txId: TxId)(implicit lh: LogHandler): Query0[ExtendedOutput] =
    sql"""
         |select distinct on (o.box_id)
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
         |  case i.main_chain when false then null else i.tx_id end
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id
         |where o.tx_id = $txId
         |""".stripMargin.query[ExtendedOutput]

  def getAllByTxIds(
    txIds: NonEmptyList[TxId]
  )(implicit lh: LogHandler): Query0[ExtendedOutput] = {
    val q =
      sql"""
           |select distinct on (o.box_id)
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
           |  case i.main_chain when false then null else i.tx_id end
           |from node_outputs o
           |left join node_inputs i on o.box_id = i.box_id
           |""".stripMargin
    (q ++ Fragments.in(fr"where o.tx_id", txIds))
      .query[ExtendedOutput]
  }

  def getAllLike(substring: String)(implicit lh: LogHandler): Query0[Address] =
    sql"select address from node_outputs where address like ${"%" + substring + "%"}"
      .query[Address]

  def sumOfAllUnspentOutputsSince(ts: Long)(implicit lh: LogHandler): Query0[BigDecimal] =
    sql"""
         |select coalesce(cast(sum(o.value) as decimal), 0)
         |from node_outputs o
         |left join (select i.box_id, i.main_chain from node_inputs i where i.main_chain = true) as i on o.box_id = i.box_id
         |where i.box_id is null and o.timestamp >= $ts
         |""".stripMargin.query[BigDecimal]

  def estimatedOutputsSince(
    ts: Long
  )(genesisAddress: Address)(implicit lh: LogHandler): Query0[BigDecimal] =
    Fragment
      .const(
        s"""
           |select coalesce(cast(sum(o.value) as decimal), 0)
           |from node_outputs o
           |join node_inputs i on o.box_id = i.box_id
           |where i.box_id is null and o.timestamp >= $ts and o.address <> '$genesisAddress'
           |""".stripMargin
      )
      .query[BigDecimal]

  def getMainUnspentSellOrderByTokenId(
    tokenId: TokenId,
    ergoTreeTemplate: HexString,
    offset: Int,
    limit: Int
  )(implicit lh: LogHandler): Query0[ExtendedOutput] =
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
         |  null
         |from node_outputs o
         |inner join node_assets a on o.box_id = a.box_id and a.token_id = $tokenId
         |left join (select i.box_id, i.main_chain from node_inputs i where i.main_chain = true) as i on o.box_id = i.box_id
         |where o.main_chain = true
         |  and (i.box_id is null or i.main_chain = false)
         |  and o.ergo_tree like ${"%" + ergoTreeTemplate}
         |offset $offset limit $limit
         |""".stripMargin.query[ExtendedOutput]

  def getMainUnspentBuyOrderByTokenId(
    tokenId: TokenId,
    ergoTreeTemplate: HexString,
    offset: Int,
    limit: Int
  )(implicit lh: LogHandler): Query0[ExtendedOutput] =
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
         |  null
         |from node_outputs o
         |left join (select i.box_id, i.main_chain from node_inputs i where i.main_chain = true) as i on o.box_id = i.box_id
         |where o.main_chain = true
         |  and (i.box_id is null or i.main_chain = false)
         |  and o.ergo_tree like ${"%" + ergoTreeTemplate}
         |  and o.ergo_tree like ${"%" + tokenId + "%"}
         |offset $offset limit $limit
         |""".stripMargin.query[ExtendedOutput]

}
