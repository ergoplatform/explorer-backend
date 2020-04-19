package org.ergoplatform.explorer.db.queries

import cats.data.NonEmptyList
import doobie.implicits._
import doobie.{Fragments, LogHandler}
import doobie.refined.implicits._
import doobie.util.query.Query0
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer.{Address, BoxId, HexString, TokenId}
import org.ergoplatform.explorer.db.models.Asset

/** A set of queries for doobie implementation of  [AssetRepo].
  */
object AssetQuerySet extends QuerySet {

  import org.ergoplatform.explorer.db.doobieInstances._

  val tableName: String = "node_assets"

  val fields: List[String] = List(
    "token_id",
    "box_id",
    "header_id",
    "value"
  )

  def getAllByBoxId(boxId: BoxId)(implicit lh: LogHandler): Query0[Asset] =
    sql"select * from node_assets where box_id = $boxId".query[Asset]

  def getAllByBoxIds(boxIds: NonEmptyList[BoxId])(implicit lh: LogHandler): Query0[Asset] =
    (sql"select * from node_assets " ++ Fragments.in(fr"where box_id", boxIds))
      .query[Asset]

  def getAllMainUnspentByErgoTree(ergoTree: HexString)(implicit lh: LogHandler): Query0[Asset] =
    sql"""
         |select a.token_id, a.box_id, a.token_id, a.value from node_assets a
         |inner join (
         |  select o.box_id from node_outputs o
         |  left join (select i.box_id, i.main_chain from node_inputs i where i.main_chain = true) as i on o.box_id = i.box_id
         |  where o.main_chain = true
         |    and (i.box_id is null or i.main_chain = false)
         |    and o.ergo_tree = $ergoTree
         |) as uo on uo.box_id = a.box_id
         """.stripMargin.query[Asset]

  def getAllHoldingAddresses(
    tokenId: TokenId,
    offset: Int,
    limit: Int
  )(implicit lh: LogHandler): Query0[Address] =
    sql"""
         |select distinct on (o.address) o.address from node_assets a
         |left join node_outputs o on a.box_id = o.box_id
         |left join node_inputs i on o.box_id = i.box_id
         |where o.main_chain = true
         |  and (i.box_id is null or i.main_chain = false)
         |  and a.token_id = $tokenId
         |offset $offset limit $limit
         |""".stripMargin.query[Address]

  /** Get boxes where tokens where issued
    * according to EIP-4 https://github.com/ergoplatform/eips/blob/master/eip-0004.md
    */
  def getAllIssuingBoxes(offset: Int, limit: Int)(implicit lh: LogHandler): Query0[ExtendedOutput] =
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
         |  i_spent.tx_id
         |from node_outputs o
         |left join node_assets a on o.box_id = a.box_id
         |left join node_inputs i_spent on o.box_id = i_spent.box_id
         |left join node_inputs i_issued on a.token_id = i_issued.box_id
         |where o.main_chain = true
         |  and i_issued.tx_id = o.tx_id
         |  and o.box_id = a.box_id
         |  and a.token_id = i_issued.box_id
         |offset $offset limit $limit
         |""".stripMargin.query[ExtendedOutput]

  def getIssuingBoxes(
    tokenIds: NonEmptyList[TokenId]
  )(implicit lh: LogHandler): Query0[ExtendedOutput] =
    (sql"""
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
          |  i_spent.tx_id
          |from node_outputs o
          |left join node_assets a on o.box_id = a.box_id
          |left join node_inputs i_spent on o.box_id = i_spent.box_id
          |left join node_inputs i_issued on a.token_id = i_issued.box_id
          |where o.main_chain = true
          |  and i_issued.tx_id = o.tx_id
          |  and o.box_id = a.box_id
          |  and a.token_id = i_issued.box_id
          |""".stripMargin ++ Fragments.in(fr"and a.token_id", tokenIds))
      .query[ExtendedOutput]
}
