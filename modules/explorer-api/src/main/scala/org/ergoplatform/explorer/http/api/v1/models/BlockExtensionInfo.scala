package org.ergoplatform.explorer.http.api.v1.models

import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Json}
import org.ergoplatform.explorer.db.models.BlockExtension
import org.ergoplatform.explorer.{BlockId, HexString}
import sttp.tapir.{Schema, SchemaType, Validator}

final case class BlockExtensionInfo(
  headerId: BlockId,
  digest: HexString,
  fields: Json
)

object BlockExtensionInfo {

  implicit private def fieldsSchema: Schema[Json] =
    Schema(SchemaType.SArray(Schema.derived[Any])(_.toIterable))

  implicit val codec: Codec[BlockExtensionInfo] = deriveCodec

  implicit val schema: Schema[BlockExtensionInfo] =
    Schema
      .derived[BlockExtensionInfo]
      .modify(_.headerId)(_.description("ID of the corresponding header"))
      .modify(_.digest)(_.description("Hex-encoded extension digest"))

  implicit val validator: Validator[BlockExtensionInfo] = schema.validator

  def apply(blockExtension: BlockExtension): BlockExtensionInfo =
    BlockExtensionInfo(
      blockExtension.headerId,
      blockExtension.digest,
      blockExtension.fields
    )
}
