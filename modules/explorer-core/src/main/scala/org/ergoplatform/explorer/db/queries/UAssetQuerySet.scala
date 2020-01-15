package org.ergoplatform.explorer.db.queries

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import org.ergoplatform.explorer.BoxId
import org.ergoplatform.explorer.db.models.UAsset

object UAssetQuerySet extends QuerySet {

  val tableName: String = "node_u_assets"

  val fields: List[String] = List(
    "token_id",
    "box_id",
    "value"
  )

  def getAllByBoxId(boxId: BoxId): ConnectionIO[List[UAsset]] =
    sql"select * from node_u_assets where box_id = $boxId".query[UAsset].to[List]
}
