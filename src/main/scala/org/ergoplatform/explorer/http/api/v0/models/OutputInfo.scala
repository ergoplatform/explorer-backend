package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Json
import sttp.tapir.{Schema, SchemaType}
import sttp.tapir.generic.Derived

final case class OutputInfo(
  id: String,
  value: Long,
  creationHeight: Int,
  ergoTree: String,
  address: String,
  assets: Seq[AssetInfo],
  additionalRegisters: Json,
  spentTxIs: Option[String],
  mainChain: Boolean
)

object OutputInfo {

  implicit private def jsonSchema: Schema[Json] =
    Schema(
      SchemaType.SOpenProduct(
        SchemaType.SObjectInfo("additionalRegisters"),
        Schema(SchemaType.SString)
      )
    )

  implicit val schema: Schema[OutputInfo] =
    implicitly[Derived[Schema[OutputInfo]]].value
}
