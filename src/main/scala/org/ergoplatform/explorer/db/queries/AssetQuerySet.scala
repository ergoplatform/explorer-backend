package org.ergoplatform.explorer.db.queries

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.refined.implicits._
import fs2.Stream
import org.ergoplatform.explorer.{Address, AssetId, BoxId}
import org.ergoplatform.explorer.db.models.Asset

/** A set of queries for doobie implementation of  [AssetRepo].
  */
object AssetQuerySet extends QuerySet {

  val tableName: String = "node_assets"

  val fields: List[String] = List(
    "id",
    "box_id",
    "header_id",
    "value"
  )

  def getAllByBoxId(boxId: BoxId): ConnectionIO[List[Asset]] =
    sql"select * from node_assets where box_id = $boxId".query[Asset].to[List]

  def getAllHoldingAddresses(
    assetId: AssetId,
    offset: Int,
    limit: Int
  ): Stream[ConnectionIO, Address] =
    sql"""
         |select distinct on (o.address) o.address from node_assets a
         |left join node_outputs o on a.box_id = o.box_id
         |left join node_inputs i on o.box_id = i.box_id
         |where o.main_chain = true
         |  and (i.box_id is null or i.main_chain = false)
         |  and a.id = $assetId
         |offset $offset limit $limit
         |""".stripMargin.query[Address].stream
}
