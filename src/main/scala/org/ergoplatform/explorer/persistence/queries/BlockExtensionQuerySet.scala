package org.ergoplatform.explorer.persistence.queries

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.refined.implicits._
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.persistence.models.BlockExtension

object BlockExtensionQuerySet extends QuerySet {

  import org.ergoplatform.explorer.persistence.doobieInstances._

  val tableName: String = "node_extensions"

  val fields: List[String] = List(
    "header_id",
    "digest",
    "fields"
  )

  def getByHeaderId(headerId: Id): ConnectionIO[Option[BlockExtension]] =
    sql"select * from node_extensions where header_id = $headerId"
      .query[BlockExtension]
      .option
}
