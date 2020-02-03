package org.ergoplatform.explorer.db.queries

import cats.data.NonEmptyList
import doobie.implicits._
import doobie.Fragments
import doobie.refined.implicits._
import doobie.util.query.Query0
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer.{Address, BoxId, TokenId}
import org.ergoplatform.explorer.db.models.Asset

/** A set of queries for doobie implementation of  [AssetRepo].
  */
object AssetQuerySet extends QuerySet {

  import org.ergoplatform.explorer.db.doobieInstances._
  import org.ergoplatform.explorer.db.models.schema.ctx._

  val tableName: String = "node_assets"

  val fields: List[String] = List(
    "token_id",
    "box_id",
    "header_id",
    "value"
  )

  def getAllByBoxId(boxId: BoxId) =
    quote {
      query[Asset].filter(_.boxId == lift(boxId))
    }

  def getAllByBoxIds(boxIds: NonEmptyList[BoxId]) =
    quote {
      query[Asset].filter(a => liftQuery(boxIds.toList).contains(a.boxId))
    }

  def getAllHoldingAddresses(
    tokenId: TokenId,
    offset: Int,
    limit: Int
  ): Query0[Address] =
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
  def getAllIssuingBoxes: Query0[ExtendedOutput] =
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
         |""".stripMargin.query[ExtendedOutput]

  def getIssuingBoxes(
    tokenIds: NonEmptyList[TokenId]
  ): Query0[ExtendedOutput] =
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
