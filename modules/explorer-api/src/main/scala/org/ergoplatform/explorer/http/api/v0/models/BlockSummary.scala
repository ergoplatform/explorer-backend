package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import sttp.tapir.{Schema, Validator}

final case class BlockSummary(block: FullBlockInfo, references: BlockReferencesInfo)

object BlockSummary {

  implicit val codec: Codec[BlockSummary] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[BlockSummary] =
    Schema
      .derived[BlockSummary]
      .modify(_.block)(_.description("Full block info"))
      .modify(_.references)(
        _.description("References to previous and next (if exists) blocks")
      )

  implicit val validator: Validator[BlockSummary] = schema.validator
}
