package org.ergoplatform.explorer.db.queries

import cats.data.NonEmptyList
import doobie.implicits._
import doobie.util.query.Query0
import doobie.{Fragments, LogHandler}
import org.ergoplatform.explorer.db.models.UAsset
import org.ergoplatform.explorer.{BoxId, HexString}

object UAssetQuerySet extends QuerySet {

  val tableName: String = "node_u_assets"

  val fields: List[String] = List(
    "token_id",
    "box_id",
    "value"
  )

  def getAllByBoxId(boxId: BoxId)(implicit lh: LogHandler): Query0[UAsset] =
    sql"select * from node_u_assets where box_id = $boxId".query[UAsset]

  def getAllByBoxIds(boxIds: NonEmptyList[BoxId])(implicit lh: LogHandler): Query0[UAsset] =
    (sql"select * from node_u_assets " ++ Fragments.in(fr"where box_id", boxIds))
      .query[UAsset]

  def getAllUnspentByErgoTree(ergoTree: HexString)(implicit lh: LogHandler): Query0[UAsset] =
    sql"""
         |select a.token_id, a.box_id, a.value from node_u_assets a
         |inner join node_u_outputs o
         |left join node_u_inputs i on i.box_id = o.box_id
         |where i.box_id is null and o.ergo_tree = $ergoTree
         """.stripMargin.query[UAsset]
}
