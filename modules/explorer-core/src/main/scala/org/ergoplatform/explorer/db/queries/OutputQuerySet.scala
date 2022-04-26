package org.ergoplatform.explorer.db.queries

import cats.data.NonEmptyList
import doobie._
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.query.Query0
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.constraints.OrderingString
import org.ergoplatform.explorer.db.models.Output
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput

/** A set of queries for doobie implementation of [OutputRepo].
  */
object OutputQuerySet extends QuerySet {

  import org.ergoplatform.explorer.db.doobieInstances._

  val tableName: String = "node_outputs"

  val fields: List[String] = List(
    "box_id",
    "tx_id",
    "header_id",
    "value",
    "creation_height",
    "settlement_height",
    "index",
    "global_index",
    "ergo_tree",
    "ergo_tree_template_hash",
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
         |  o.header_id,
         |  o.value,
         |  o.creation_height,
         |  o.settlement_height,
         |  o.index,
         |  o.global_index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain,
         |  i.tx_id
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |where o.box_id = $boxId
         |order by o.main_chain desc limit 1
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
         |  o.header_id,
         |  o.value,
         |  o.creation_height,
         |  o.settlement_height,
         |  o.index,
         |  o.global_index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain,
         |  i.tx_id
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |where o.main_chain = true and o.ergo_tree = $ergoTree
         |offset $offset limit $limit
         |""".stripMargin.query[ExtendedOutput]

  def countAllByErgoTree(
    ergoTree: HexString
  )(implicit lh: LogHandler): Query0[Int] =
    sql"""
         |select count(distinct o.box_id)
         |from node_outputs o
         |where o.main_chain = true and o.ergo_tree = $ergoTree
         |""".stripMargin.query[Int]

  def getMainByErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int,
    maxHeight: Int
  )(implicit lh: LogHandler): Query0[ExtendedOutput] =
    sql"""
         |select
         |  o.box_id,
         |  o.tx_id,
         |  o.header_id,
         |  o.value,
         |  o.creation_height,
         |  o.settlement_height,
         |  o.index,
         |  o.global_index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain,
         |  i.tx_id
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |left join node_transactions tx on tx.id = o.tx_id
         |where o.main_chain = true
         |  and tx.inclusion_height <= $maxHeight
         |  and o.ergo_tree = $ergoTree
         |offset $offset limit $limit
         |""".stripMargin.query[ExtendedOutput]

  def sumAllByErgoTree(
    ergoTree: HexString,
    maxHeight: Int
  )(implicit lh: LogHandler): Query0[Long] =
    sql"""
         |select coalesce(cast(sum(o.value) as bigint), 0)
         |from node_outputs o
         |left join node_transactions tx on tx.id = o.tx_id
         |where tx.main_chain = true
         |  and tx.inclusion_height <= $maxHeight
         |  and o.main_chain = true
         |  and o.ergo_tree = $ergoTree
         |""".stripMargin.query[Long]

  def sumUnspentByErgoTree(
    ergoTree: HexString,
    maxHeight: Int
  )(implicit lh: LogHandler): Query0[Long] =
    sql"""
         |select coalesce(cast(sum(o.value) as bigint), 0) from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |left join node_transactions tx on tx.id = o.tx_id
         |where tx.main_chain = true
         |  and tx.inclusion_height <= $maxHeight
         |  and o.main_chain = true
         |  and i.box_id is null
         |  and o.ergo_tree = $ergoTree
         |""".stripMargin.query[Long]

  def getAllMainUnspentIdsByErgoTree(
    ergoTree: HexString
  )(implicit lh: LogHandler): Query0[BoxId] =
    sql"""
         |select o.box_id from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |where o.main_chain = true
         |  and i.box_id is null
         |  and o.ergo_tree = $ergoTree
         |""".stripMargin.query[BoxId]

  def balanceStatsMain(offset: Int, limit: Int)(implicit lh: LogHandler): Query0[(Address, Long)] =
    sql"""
         |select o.address, coalesce(cast(sum(o.value) as bigint), 0) as balance from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |left join node_transactions tx on tx.id = o.tx_id
         |where o.main_chain = true and i.box_id is null
         |group by o.address
         |order by balance desc
         |offset $offset limit $limit
         |""".stripMargin.query[(Address, Long)]

  def totalAddressesMain(implicit lh: LogHandler): Query0[Int] =
    sql"""
         |select count(*) from (
         |  select distinct o.address from node_outputs o
         |  left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |  left join node_transactions tx on tx.id = o.tx_id
         |  where o.main_chain = true and i.box_id is null
         |) as _
         |""".stripMargin.query[Int]

  def getMainUnspentByErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int,
    ordering: OrderingString
  )(implicit lh: LogHandler): Query0[ExtendedOutput] = {
    val q   = sql"""
         |select distinct on (o.box_id, o.global_index)
         |  o.box_id,
         |  o.tx_id,
         |  o.header_id,
         |  o.value,
         |  o.creation_height,
         |  o.settlement_height,
         |  o.index,
         |  o.global_index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain,
         |  null
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |where o.main_chain = true
         |  and i.box_id is null
         |  and o.ergo_tree = $ergoTree
         |""".stripMargin
    val ord = Fragment.const(s"order by o.global_index $ordering")
    val lim = Fragment.const(s"offset $offset limit $limit")
    (q ++ ord ++ lim).query
  }

  def countUnspentByErgoTree(
    ergoTree: HexString
  )(implicit lh: LogHandler): Query0[Int] =
    sql"""
         |select count(distinct o.box_id)
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |where o.main_chain = true
         |  and i.box_id is null
         |  and o.ergo_tree = $ergoTree
         |""".stripMargin.query[Int]

  def getAllByTxId(txId: TxId)(implicit lh: LogHandler): Query0[ExtendedOutput] =
    sql"""
         |select distinct on (o.index, o.box_id)
         |  o.box_id,
         |  o.tx_id,
         |  o.header_id,
         |  o.value,
         |  o.creation_height,
         |  o.settlement_height,
         |  o.index,
         |  o.global_index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain,
         |  i.tx_id
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |where o.tx_id = $txId
         |order by o.index asc
         |""".stripMargin.query[ExtendedOutput]

  def getAllByTxIds(
    txIds: NonEmptyList[TxId]
  )(implicit lh: LogHandler): Query0[ExtendedOutput] = {
    val q =
      sql"""
           |select distinct on (o.box_id)
           |  o.box_id,
           |  o.tx_id,
           |  o.header_id,
           |  o.value,
           |  o.creation_height,
           |  o.settlement_height,
           |  o.index,
           |  o.global_index,
           |  o.ergo_tree,
           |  o.ergo_tree_template_hash,
           |  o.address,
           |  o.additional_registers,
           |  o.timestamp,
           |  o.main_chain,
           |  i.tx_id
           |from node_outputs o
           |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
           |""".stripMargin
    (q ++ Fragments.in(fr"where o.tx_id", txIds))
      .query[ExtendedOutput]
  }

  def getAllByTxIds(
    txIds: NonEmptyList[TxId],
    narrowByAddress: Address
  )(implicit lh: LogHandler): Query0[ExtendedOutput] = {
    val q =
      sql"""
           |select distinct on (o.box_id)
           |  o.box_id,
           |  o.tx_id,
           |  o.header_id,
           |  o.value,
           |  o.creation_height,
           |  o.settlement_height,
           |  o.index,
           |  o.global_index,
           |  o.ergo_tree,
           |  o.ergo_tree_template_hash,
           |  o.address,
           |  o.additional_registers,
           |  o.timestamp,
           |  o.main_chain,
           |  i.tx_id
           |from node_outputs o
           |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
           |""".stripMargin
    (q ++ Fragments.in(fr"where o.address = $narrowByAddress and o.tx_id", txIds))
      .query[ExtendedOutput]
  }

  def getAllLike(substring: String)(implicit lh: LogHandler): Query0[Address] =
    sql"select distinct address from node_outputs where address like ${"%" + substring + "%"}"
      .query[Address]

  def sumOfAllUnspentOutputsSince(ts: Long)(implicit lh: LogHandler): Query0[BigDecimal] =
    sql"""
         |select coalesce(cast(sum(o.value) as decimal), 0)
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
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

  def updateChainStatusByHeaderId(headerId: BlockId, newChainStatus: Boolean)(implicit lh: LogHandler): Update0 =
    sql"""
         |update node_outputs set main_chain = $newChainStatus
         |where header_id = $headerId
         """.stripMargin.update

  def getUnspent(minHeight: Int, maxHeight: Int)(implicit lh: LogHandler): Query0[Output] =
    sql"""
         |select
         |  o.box_id,
         |  o.tx_id,
         |  o.header_id,
         |  o.value,
         |  o.creation_height,
         |  o.settlement_height,
         |  o.index,
         |  o.global_index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |left join node_headers h on h.id = o.header_id
         |where o.main_chain = true
         |  and i.box_id is null
         |  and h.height >= $minHeight
         |  and h.height <= $maxHeight
         |order by o.global_index asc
         |""".stripMargin.query[Output]

  def getUnspent(minGix: Long, limit: Int)(implicit lh: LogHandler): Query0[Output] =
    sql"""
         |select
         |  o.box_id,
         |  o.tx_id,
         |  o.header_id,
         |  o.value,
         |  o.creation_height,
         |  o.settlement_height,
         |  o.index,
         |  o.global_index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |left join node_headers h on h.id = o.header_id
         |where o.main_chain = true
         |  and i.box_id is null
         |  and o.global_index >= $minGix
         |  and o.global_index < ${minGix + limit}
         |order by o.global_index asc
         |""".stripMargin.query[Output]

  def getAll(minGix: Long, limit: Int)(implicit lh: LogHandler): Query0[Output] =
    sql"""
         |select
         |  o.box_id,
         |  o.tx_id,
         |  o.header_id,
         |  o.value,
         |  o.creation_height,
         |  o.settlement_height,
         |  o.index,
         |  o.global_index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain
         |from node_outputs o
         |left join node_headers h on h.id = o.header_id
         |where o.main_chain = true
         |  and o.global_index >= $minGix
         |  and o.global_index < ${minGix + limit}
         |order by o.global_index asc
         |""".stripMargin.query[Output]

  def getAllByTokenId(tokenId: TokenId, offset: Int, limit: Int)(implicit lh: LogHandler): Query0[ExtendedOutput] =
    sql"""
         |select distinct on (o.box_id, o.creation_height)
         |  o.box_id,
         |  o.tx_id,
         |  o.header_id,
         |  o.value,
         |  o.creation_height,
         |  o.settlement_height,
         |  o.index,
         |  o.global_index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain,
         |  i.tx_id
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |left join node_assets a on o.box_id = a.box_id
         |where a.token_id = $tokenId and o.main_chain = true
         |order by o.creation_height asc
         |offset $offset limit $limit
         |""".stripMargin.query[ExtendedOutput]

  def countAllByTokenId(tokenId: TokenId)(implicit lh: LogHandler): Query0[Int] =
    sql"""
         |select count(distinct o.box_id) from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id
         |left join node_assets a on o.box_id = a.box_id
         |where a.token_id = $tokenId and o.main_chain = true
         |""".stripMargin.query[Int]

  def getUnspentByTokenId(tokenId: TokenId, offset: Int, limit: Int, ordering: OrderingString)(implicit
    lh: LogHandler
  ): Query0[Output] =
    (sql"""
         |select distinct on (o.box_id, o.global_index)
         |  o.box_id,
         |  o.tx_id,
         |  o.header_id,
         |  o.value,
         |  o.creation_height,
         |  o.settlement_height,
         |  o.index,
         |  o.global_index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |left join node_assets a on o.box_id = a.box_id
         |where a.token_id = $tokenId
         |  and i.box_id is null
         |  and o.main_chain = true
         |""".stripMargin ++
      Fragment.const(s"order by o.global_index $ordering offset $offset limit $limit")).query[Output]

  def countUnspentByTokenId(tokenId: TokenId)(implicit lh: LogHandler): Query0[Int] =
    sql"""
         |select count(distinct o.box_id) from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |left join node_assets a on o.box_id = a.box_id
         |where a.token_id = $tokenId
         |  and i.box_id is null
         |  and o.main_chain = true
         |""".stripMargin.query[Int]

  def getAllByErgoTreeTemplateHash(templateHash: ErgoTreeTemplateHash, offset: Int, limit: Int)(implicit
    lh: LogHandler
  ): Query0[ExtendedOutput] =
    sql"""
         |select distinct on (o.box_id, o.creation_height)
         |  o.box_id,
         |  o.tx_id,
         |  o.header_id,
         |  o.value,
         |  o.creation_height,
         |  o.settlement_height,
         |  o.index,
         |  o.global_index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain,
         |  i.tx_id
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |where o.ergo_tree_template_hash = $templateHash
         |order by o.creation_height asc
         |offset $offset limit $limit
         |""".stripMargin.query[ExtendedOutput]

  def countAllByErgoTreeTemplateHash(templateHash: ErgoTreeTemplateHash)(implicit
    lh: LogHandler
  ): Query0[Int] =
    sql"""
         |select count(distinct o.box_id)
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id
         |where o.ergo_tree_template_hash = $templateHash
         |""".stripMargin.query[Int]

  def searchAll(
    templateHash: ErgoTreeTemplateHash,
    registers: Option[NonEmptyList[(RegisterId, String)]],
    constants: Option[NonEmptyList[(Int, String)]],
    assets: Option[NonEmptyList[TokenId]],
    offset: Int,
    limit: Int
  )(implicit
    lh: LogHandler
  ): Query0[ExtendedOutput] =
    Fragment
      .const(
        s"""
           |select distinct on (o.box_id, o.creation_height)
           |  o.box_id,
           |  o.tx_id,
           |  o.header_id,
           |  o.value,
           |  o.creation_height,
           |  o.settlement_height,
           |  o.index,
           |  o.global_index,
           |  o.ergo_tree,
           |  o.ergo_tree_template_hash,
           |  o.address,
           |  o.additional_registers,
           |  o.timestamp,
           |  o.main_chain,
           |  i.tx_id
           |from node_outputs o
           |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
           |${registers.map(innerJoinAllOfRegisters(as = "rs", tableAlias = "o", _)).getOrElse("")}
           |${constants.map(innerJoinAllOfConstants(as = "sc", tableAlias = "o", _)).getOrElse("")}
           |${assets.map(innerJoinAllOfAssets(as = "ts", tableAlias = "o", _)).getOrElse("")}
           |where o.ergo_tree_template_hash = '$templateHash' and o.main_chain = true
           |order by o.creation_height asc
           |offset $offset limit $limit
           |""".stripMargin
      )
      .query[ExtendedOutput]

  def countAll(
    templateHash: ErgoTreeTemplateHash,
    registers: Option[NonEmptyList[(RegisterId, String)]],
    constants: Option[NonEmptyList[(Int, String)]],
    assets: Option[NonEmptyList[TokenId]]
  )(implicit
    lh: LogHandler
  ): Query0[Int] =
    Fragment
      .const(
        s"""
           |select count(distinct o.box_id) from node_outputs o
           |${registers.map(innerJoinAllOfRegisters(as = "rs", tableAlias = "o", _)).getOrElse("")}
           |${constants.map(innerJoinAllOfConstants(as = "sc", tableAlias = "o", _)).getOrElse("")}
           |${assets.map(innerJoinAllOfAssets(as = "ts", tableAlias = "o", _)).getOrElse("")}
           |where o.ergo_tree_template_hash = '$templateHash' and o.main_chain = true
           |""".stripMargin
      )
      .query[Int]

  def searchUnspent(
    templateHash: ErgoTreeTemplateHash,
    registers: Option[NonEmptyList[(RegisterId, String)]],
    constants: Option[NonEmptyList[(Int, String)]],
    assets: Option[NonEmptyList[TokenId]],
    offset: Int,
    limit: Int
  )(implicit
    lh: LogHandler
  ): Query0[Output] =
    Fragment
      .const(
        s"""
           |select distinct on (o.box_id, o.creation_height)
           |  o.box_id,
           |  o.tx_id,
           |  o.header_id,
           |  o.value,
           |  o.creation_height,
           |  o.settlement_height,
           |  o.index,
           |  o.global_index,
           |  o.ergo_tree,
           |  o.ergo_tree_template_hash,
           |  o.address,
           |  o.additional_registers,
           |  o.timestamp,
           |  o.main_chain
           |from node_outputs o
           |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
           |${registers.map(innerJoinAllOfRegisters(as = "rs", tableAlias = "o", _)).getOrElse("")}
           |${constants.map(innerJoinAllOfConstants(as = "sc", tableAlias = "o", _)).getOrElse("")}
           |${assets.map(innerJoinAllOfAssets(as = "ts", tableAlias = "o", _)).getOrElse("")}
           |where o.ergo_tree_template_hash = '$templateHash' and o.main_chain = true and i.tx_id is null
           |order by o.creation_height asc
           |offset $offset limit $limit
           |""".stripMargin
      )
      .query[Output]

  def countUnspent(
    templateHash: ErgoTreeTemplateHash,
    registers: Option[NonEmptyList[(RegisterId, String)]],
    constants: Option[NonEmptyList[(Int, String)]],
    assets: Option[NonEmptyList[TokenId]]
  )(implicit
    lh: LogHandler
  ): Query0[Int] =
    Fragment
      .const(
        s"""
           |select count(distinct o.box_id) from node_outputs o
           |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
           |${registers.map(innerJoinAllOfRegisters(as = "rs", tableAlias = "o", _)).getOrElse("")}
           |${constants.map(innerJoinAllOfConstants(as = "sc", tableAlias = "o", _)).getOrElse("")}
           |${assets.map(innerJoinAllOfAssets(as = "ts", tableAlias = "o", _)).getOrElse("")}
           |where o.ergo_tree_template_hash = '$templateHash' and o.main_chain = true and i.tx_id is null
           |""".stripMargin
      )
      .query[Int]

  def searchUnspentByAssetsUnion(
    templateHash: ErgoTreeTemplateHash,
    assets: List[TokenId],
    offset: Int,
    limit: Int
  )(implicit
    lh: LogHandler
  ): Query0[Output] =
    Fragment
      .const(
        s"""
           |select distinct on (o.box_id, o.creation_height)
           |  o.box_id,
           |  o.tx_id,
           |  o.header_id,
           |  o.value,
           |  o.creation_height,
           |  o.settlement_height,
           |  o.index,
           |  o.global_index,
           |  o.ergo_tree,
           |  o.ergo_tree_template_hash,
           |  o.address,
           |  o.additional_registers,
           |  o.timestamp,
           |  o.main_chain
           |from node_outputs o
           |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
           |left join node_assets ts on o.box_id = ts.box_id
           |where
           |  o.ergo_tree_template_hash = '$templateHash' and
           |  o.main_chain = true and
           |  i.tx_id is null and
           |  ts.token_id in (${assets.map(s => s"'$s'").mkString(", ")})
           |order by o.creation_height asc
           |offset $offset limit $limit
           |""".stripMargin
      )
      .query[Output]

  def countUnspentByAssetsUnion(
    templateHash: ErgoTreeTemplateHash,
    assets: List[TokenId]
  )(implicit
    lh: LogHandler
  ): Query0[Int] =
    Fragment
      .const(
        s"""
           |select count(distinct o.box_id) from node_outputs o
           |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
           |left join node_assets ts on o.box_id = ts.box_id
           |where
           |  o.ergo_tree_template_hash = '$templateHash' and
           |  o.main_chain = true and
           |  i.tx_id is null and
           |  ts.token_id in (${assets.map(s => s"'$s'").mkString(", ")})
           |""".stripMargin
      )
      .query[Int]

  def getUnspentByErgoTreeTemplateHash(templateHash: ErgoTreeTemplateHash, offset: Int, limit: Int)(implicit
    lh: LogHandler
  ): Query0[Output] =
    sql"""
         |select distinct on (o.box_id, o.header_id, o.creation_height)
         |  o.box_id,
         |  o.tx_id,
         |  o.header_id,
         |  o.value,
         |  o.creation_height,
         |  o.settlement_height,
         |  o.index,
         |  o.global_index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |where o.main_chain = true
         |  and i.box_id is null
         |  and o.ergo_tree_template_hash = $templateHash
         |order by o.creation_height asc
         |offset $offset limit $limit
         |""".stripMargin.query[Output]

  def countUnspentByErgoTreeTemplateHash(templateHash: ErgoTreeTemplateHash)(implicit
    lh: LogHandler
  ): Query0[Int] =
    sql"""
         |select count(distinct o.box_id)
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |where o.main_chain = true
         |  and i.box_id is null
         |  and o.ergo_tree_template_hash = $templateHash
         |""".stripMargin.query[Int]

  def getUnspentByErgoTreeTemplateHashAndTokenId(
    templateHash: ErgoTreeTemplateHash,
    tokenId: TokenId,
    offset: Int,
    limit: Int
  )(implicit
    lh: LogHandler
  ): Query0[ExtendedOutput] =
    sql"""
         |select distinct on (o.box_id, o.header_id, o.creation_height)
         |  o.box_id,
         |  o.tx_id,
         |  o.header_id,
         |  o.value,
         |  o.creation_height,
         |  o.settlement_height,
         |  o.index,
         |  o.global_index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain,
         |  null
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |left join node_assets a on o.box_id = a.box_id
         |where o.main_chain = true
         |  and i.box_id is null
         |  and a.token_id = $tokenId
         |  and o.ergo_tree_template_hash = $templateHash
         |order by o.creation_height asc
         |offset $offset limit $limit
         |""".stripMargin.query[ExtendedOutput]

  def getAllByErgoTreeTemplateHashByEpochs(templateHash: ErgoTreeTemplateHash, minHeight: Int, maxHeight: Int)(implicit
    lh: LogHandler
  ): Query0[ExtendedOutput] =
    sql"""
         |select distinct on (o.box_id, o.creation_height)
         |  o.box_id,
         |  o.tx_id,
         |  o.header_id,
         |  o.value,
         |  o.creation_height,
         |  o.settlement_height,
         |  o.index,
         |  o.global_index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain,
         |  i.tx_id
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |left join node_headers h on h.id = o.header_id
         |where o.ergo_tree_template_hash = $templateHash
         |  and h.height >= $minHeight
         |  and h.height <= $maxHeight
         |order by h.height asc
         |""".stripMargin.query[ExtendedOutput]

  def getUnspentByErgoTreeTemplateHashByEpochs(templateHash: ErgoTreeTemplateHash, minHeight: Int, maxHeight: Int)(
    implicit lh: LogHandler
  ): Query0[Output] =
    sql"""
         |select distinct on (o.box_id, o.creation_height)
         |  o.box_id,
         |  o.tx_id,
         |  o.header_id,
         |  o.value,
         |  o.creation_height,
         |  o.settlement_height,
         |  o.index,
         |  o.global_index,
         |  o.ergo_tree,
         |  o.ergo_tree_template_hash,
         |  o.address,
         |  o.additional_registers,
         |  o.timestamp,
         |  o.main_chain
         |from node_outputs o
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |where o.main_chain = true
         |  and i.box_id is null
         |  and o.ergo_tree_template_hash = $templateHash
         |  and h.height >= $minHeight
         |  and h.height <= $maxHeight
         |order by h.height asc
         |""".stripMargin.query[Output]
}
