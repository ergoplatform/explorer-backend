package org.ergoplatform.explorer.db.models

import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Json}
import org.ergoplatform.explorer.{HexString, Id}
import sttp.tapir.{Schema, SchemaType}

/** Represents `node_extensions` table.
 */
final case class BlockExtension(
  headerId: Id,
  digest: HexString,
  fields: Json       // arbitrary key->value dictionary
)

object BlockExtension {

  implicit val codec: Codec[BlockExtension] = deriveCodec

  implicit def schema: Schema[BlockExtension] =
    Schema.derive

  implicit private def fieldsSchema: Schema[Json] =
    Schema(
      SchemaType.SOpenProduct(
        SchemaType.SObjectInfo("Fields"),
        Schema(SchemaType.SString)
      )
    )
}
