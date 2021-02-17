package org.ergoplatform.explorer.db.queries

object ScriptConstantsQuerySet extends QuerySet {

  val tableName: String = "script_constants"

  val fields: List[String] = List(
    "index",
    "box_id",
    "value_type",
    "serialized_value",
    "rendered_value"
  )
}
