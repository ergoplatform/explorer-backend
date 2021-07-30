package org.ergoplatform.explorer.db.models

import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import io.circe.{Codec, Json}
import org.ergoplatform.explorer.{HexString, BlockId}
import sttp.tapir.{Schema, SchemaType}

/** Represents `node_extensions` table.
 */
final case class BlockExtension(
                                 headerId: BlockId,
                                 digest: HexString,
                                 fields: Json // arbitrary key->value dictionary
)

object BlockExtension {

  implicit val codec: Codec[BlockExtension] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit def schema: Schema[BlockExtension] = Schema.derived[BlockExtension]

  //todo: test
  implicit private def fieldsSchema: Schema[Json] =
    Schema(
      SchemaType.SOpenProduct(
        SchemaType.SObjectInfo("Fields"),
        Schema.schemaForString
      )(_ => Map.empty)
    )
}
