package org.ergoplatform.explorer.db.queries

object BoxRegisterQuerySet extends QuerySet {

  val tableName: String = "box_registers"

  val fields: List[String] = List(
    "id",
    "box_id",
    "value_type",
    "serialized_value",
    "rendered_value"
  )
}
