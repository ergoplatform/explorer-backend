package org.ergoplatform.explorer.db.queries

import cats.data.NonEmptyList
import doobie.implicits._
import doobie.util.query.Query0
import doobie.{Fragments, LogHandler}
import org.ergoplatform.explorer.db.models.aggregates.ExtendedUAsset
import org.ergoplatform.explorer.{BoxId, HexString}

object UAssetQuerySet extends QuerySet {

  val tableName: String = "node_u_assets"

  val fields: List[String] = List(
    "token_id",
    "box_id",
    "index",
    "value"
  )

  def getAllByBoxId(boxId: BoxId)(implicit lh: LogHandler): Query0[ExtendedUAsset] =
    sql"""
         |select
         |  a.token_id,
         |  a.box_id,
         |  a.index,
         |  a.value,
         |  t.name,
         |  t.decimals,
         |  t.type
         |from node_u_assets a
         |left join tokens t on a.token_id = t.token_id
         |where box_id = $boxId
         |""".query[ExtendedUAsset]

  def getAllByBoxIds(boxIds: NonEmptyList[BoxId])(implicit lh: LogHandler): Query0[ExtendedUAsset] =
    (
      sql"""
           |select distinct
           |  a.token_id,
           |  a.box_id,
           |  a.index,
           |  a.value,
           |  t.name,
           |  t.decimals,
           |  t.type
           |from node_u_assets a
           |left join tokens t on a.token_id = t.token_id
           |""" ++
    Fragments.in(fr"where a.box_id", boxIds))
      .query[ExtendedUAsset]

  def getAllUnspentByErgoTree(ergoTree: HexString)(implicit lh: LogHandler): Query0[ExtendedUAsset] =
    sql"""
         |select
         |  a.token_id,
         |  a.box_id,
         |  a.index,
         |  a.value,
         |  t.name,
         |  t.decimals,
         |  t.type
         |from node_u_assets a
         |left join tokens t on a.token_id = t.token_id
         |inner join (
         |  select o.box_id from node_u_outputs o
         |  left join node_u_inputs i on i.box_id = o.box_id
         |  where i.box_id is null and o.ergo_tree = $ergoTree
         |) as uo on uo.box_id = a.box_id
         """.stripMargin.query[ExtendedUAsset]
}
