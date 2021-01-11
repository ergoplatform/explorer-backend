package org.ergoplatform.explorer.db.queries

object TokensQuerySet extends QuerySet {

  val tableName: String = "tokens"

  val fields: List[String] = List(
    "id",
    "name",
    "description",
    "type",
    "decimals",
    "emission_amount"
  )
}
