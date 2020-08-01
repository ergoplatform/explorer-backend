package org.ergoplatform.explorer.db.models

import io.circe.{Codec, Json}
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.protocol.models.ApiBlockExtension
import org.ergoplatform.explorer.{HexString, Id}
import sttp.tapir.generic.Derived
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
    implicitly[Derived[Schema[BlockExtension]]].value

  implicit private def fieldsSchema: Schema[Json] =
    Schema(
      SchemaType.SOpenProduct(
        SchemaType.SObjectInfo("Fields"),
        Schema(SchemaType.SString)
      )
    )

  def fromApi(apiExtension: ApiBlockExtension): BlockExtension =
    BlockExtension(
      apiExtension.headerId,
      apiExtension.digest,
      apiExtension.fields
    )
}
