package org.ergoplatform.explorer.http.api.v0.models

import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
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

  implicit val codec: Codec[BlockExtensionInfo] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit private def registersSchema: Schema[Json] =
    Schema(
      SchemaType.SOpenProduct(
        Schema(SchemaType.SString[Json]())
      )(_ => Map.empty)
    )

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
