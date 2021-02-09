package org.ergoplatform.explorer.db.queries

object ScriptConstantsQuerySet extends QuerySet {

  override val tableName: String = "script_constants"

  override val fields: List[String] = List(
    "index",
    "box_id",
    "sigma_type",
    "serialized_value",
    "rendered_value"
  )
}
