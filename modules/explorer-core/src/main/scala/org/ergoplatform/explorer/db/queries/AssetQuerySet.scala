package org.ergoplatform.explorer.db.queries

import cats.data.NonEmptyList
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.query.Query0
import doobie.{Fragments, LogHandler}
import org.ergoplatform.explorer.db.models.aggregates.{AggregatedAsset, ExtendedAsset, ExtendedOutput}
import org.ergoplatform.explorer.{Address, BoxId, HexString, TokenId}

/** A set of queries for doobie implementation of  [AssetRepo].
  */
object AssetQuerySet extends QuerySet {

  import org.ergoplatform.explorer.db.doobieInstances._

  val tableName: String = "node_assets"

  val fields: List[String] = List(
    "token_id",
    "box_id",
    "header_id",
    "index",
    "value"
  )

  def getAllByBoxId(boxId: BoxId)(implicit lh: LogHandler): Query0[ExtendedAsset] =
    sql"""
         |select distinct on (a.index, a.token_id, a.box_id)
         |  a.token_id,
         |  a.box_id,
         |  a.header_id,
         |  a.index,
         |  a.value,
         |  t.name,
         |  t.decimals,
         |  t.type
         |from node_assets a
         |left join tokens t on a.token_id = t.token_id
         |where a.box_id = $boxId
         |""".stripMargin.query[ExtendedAsset]

  def getAllByBoxIds(boxIds: NonEmptyList[BoxId])(implicit lh: LogHandler): Query0[ExtendedAsset] =
    (sql"""
           |select distinct on (a.index, a.token_id, a.box_id)
           |  a.token_id,
           |  a.box_id,
           |  a.header_id,
           |  a.index,
           |  a.value,
           |  t.name,
           |  t.decimals,
           |  t.type
           |from node_assets a
           |left join tokens t on a.token_id = t.token_id
           |""".stripMargin
      ++ Fragments.in(fr"where a.box_id", boxIds))
      .query[ExtendedAsset]

  def getAllMainUnspentByErgoTree(ergoTree: HexString)(implicit lh: LogHandler): Query0[ExtendedAsset] =
    sql"""
         |select distinct on (a.index, a.token_id, a.box_id)
         |  a.token_id,
         |  a.box_id,
         |  a.header_id,
         |  a.index,
         |  a.value,
         |  t.name,
         |  t.decimals,
         |  t.type
         |from node_assets a
         |left join tokens t on a.token_id = t.token_id
         |inner join (
         |  select o.box_id from node_outputs o
         |  left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |  where o.main_chain = true
         |    and i.box_id is null
         |    and o.ergo_tree = $ergoTree
         |) as uo on uo.box_id = a.box_id
         """.stripMargin.query[ExtendedAsset]

  def aggregateUnspentByErgoTree(ergoTree: HexString, maxHeight: Int)(implicit
    lh: LogHandler
  ): Query0[AggregatedAsset] =
    sql"""
         |select agg.token_id, agg.total, t.name, t.decimals, t.type from (
         |  select ia.token_id, sum(ia.value) as total from (
         |    select distinct on (a.index, a.token_id, a.box_id)
         |      a.token_id,
         |      a.box_id,
         |      a.value
         |    from node_assets a
         |    inner join (
         |      select distinct o.box_id from node_outputs o
         |      left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |      left join node_transactions tx on tx.id = o.tx_id and tx.main_chain = true
         |      where tx.inclusion_height <= $maxHeight
         |        and o.main_chain = true
         |        and i.box_id is null
         |        and o.ergo_tree = $ergoTree
         |    ) as uo on uo.box_id = a.box_id
         |  ) as ia
         |  group by ia.token_id
         |) agg
         |left join tokens t on t.token_id = agg.token_id
         |""".stripMargin.query[AggregatedAsset]

  def getAllHoldingAddresses(
    tokenId: TokenId,
    offset: Int,
    limit: Int
  )(implicit lh: LogHandler): Query0[Address] =
    sql"""
         |select distinct on (o.address) o.address from node_assets a
         |left join node_outputs o on a.box_id = o.box_id
         |left join node_inputs i on o.box_id = i.box_id and i.main_chain = true
         |where o.main_chain = true
         |  and i.box_id is null
         |  and a.token_id = $tokenId
         |offset $offset limit $limit
         |""".stripMargin.query[Address]

  /** Get boxes where tokens where issued
    * according to EIP-4 https://github.com/ergoplatform/eips/blob/master/eip-0004.md
    */
  def getAllIssuingBoxes(offset: Int, limit: Int)(implicit lh: LogHandler): Query0[ExtendedOutput] =
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
         |  i_spent.tx_id
         |from node_outputs o
         |left join node_assets a on o.box_id = a.box_id
         |left join node_inputs i_spent on o.box_id = i_spent.box_id and i_spent.main_chain = true
         |left join node_inputs i_issued on a.token_id = i_issued.box_id and i_issued.main_chain = true
         |where o.main_chain = true
         |  and i_issued.tx_id = o.tx_id
         |  and o.box_id = a.box_id
         |  and a.token_id = i_issued.box_id
         |order by o.creation_height asc
         |offset $offset limit $limit
         |""".stripMargin.query[ExtendedOutput]

  def getIssuingBoxes(
    tokenIds: NonEmptyList[TokenId]
  )(implicit lh: LogHandler): Query0[ExtendedOutput] =
    (sql"""
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
          |  i_spent.tx_id
          |from node_outputs o
          |left join node_assets a on o.box_id = a.box_id
          |left join node_inputs i_spent on o.box_id = i_spent.box_id and i_spent.main_chain = true
          |left join node_inputs i_issued on a.token_id = i_issued.box_id and i_issued.main_chain = true
          |where o.main_chain = true
          |  and i_issued.tx_id = o.tx_id
          |  and o.box_id = a.box_id
          |  and a.token_id = i_issued.box_id
          |""".stripMargin ++ Fragments.in(fr"and a.token_id", tokenIds) ++ sql"order by o.creation_height asc")
      .query[ExtendedOutput]

  def getIssuingBoxesQty(implicit lh: LogHandler): Query0[Int] =
    sql"""
         |select count(a.token_id) from (
         |  select distinct on (a.token_id) * from node_assets a
         |  left join node_outputs o on o.box_id = a.box_id where o.main_chain = true
         |) as a
         """.stripMargin.query[Int]

  def getAllLike(idSubstring: String, offset: Int, limit: Int)(implicit lh: LogHandler): Query0[ExtendedAsset] =
    sql"""
         |select a.token_id, a.box_id, a.header_id, a.index, a.value, t.name, t.decimals, t.type from node_assets a
         |left join tokens t on a.token_id = t.token_id
         |left join node_outputs o on a.box_id = o.box_id
         |where a.token_id like ${idSubstring + "%"} and o.main_chain = true
         |offset $offset limit $limit
         """.stripMargin.query[ExtendedAsset]

  def countAllLike(idSubstring: String)(implicit lh: LogHandler): Query0[Int] =
    sql"""
         |select count(*) from node_assets a
         |left join node_outputs o on a.box_id = o.box_id
         |where a.token_id like ${idSubstring + "%"} and o.main_chain = true
         """.stripMargin.query[Int]
}
