package org.ergoplatform.explorer.http.api.v0.models

import io.circe.{Codec, Json}
import io.circe.generic.semiauto.deriveCodec
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

  implicit val codec: Codec[OutputInfo] = deriveCodec

  implicit val schema: Schema[OutputInfo] =
    implicitly[Derived[Schema[OutputInfo]]].value

  implicit private def jsonSchema: Schema[Json] =
    Schema(
      SchemaType.SOpenProduct(
        SchemaType.SObjectInfo("AdditionalRegisters"),
        Schema(SchemaType.SString)
      )
    )
}
