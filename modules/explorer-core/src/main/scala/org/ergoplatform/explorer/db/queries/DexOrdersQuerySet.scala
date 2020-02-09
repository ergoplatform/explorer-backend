package org.ergoplatform.explorer.db.queries

import doobie.Fragments
import doobie.util.query.Query0
import doobie.implicits._
import doobie.refined.implicits._
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.db.models.{Asset, Input, Output}
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput

/** A set of queries for doobie implementation of [DexOrdersRepo].
  */
object DexOrdersQuerySet {

  import org.ergoplatform.explorer.db.doobieInstances._

  def getMainUnspentSellOrderByTokenId(
    tokenId: TokenId,
    ergoTreeTemplate: HexString,
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
         |inner join node_assets a on o.box_id = a.box_id and a.token_id = $tokenId
         |left join node_inputs i on o.box_id = i.box_id
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
         |  and o.ergo_tree like ${"%" + ergoTreeTemplate}
         |  and o.ergo_tree like ${"%" + tokenId + "%"}
         |offset $offset limit $limit
         |""".stripMargin.query[ExtendedOutput]

}
