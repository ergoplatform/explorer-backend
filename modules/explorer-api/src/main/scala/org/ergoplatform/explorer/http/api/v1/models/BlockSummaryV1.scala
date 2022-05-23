package org.ergoplatform.explorer.http.api.v1.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.http.api.v0.models.BlockReferencesInfo
import sttp.tapir.{Schema, Validator}

final case class BlockSummaryV1(block: FullBlockInfoV1, references: BlockReferencesInfo)

object BlockSummaryV1 {

  implicit val codec: Codec[BlockSummaryV1] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[BlockSummaryV1] =
    Schema
      .derived[BlockSummaryV1]
      .modify(_.block)(_.description("Full block info"))
      .modify(_.references)(
        _.description("References to previous and next (if exists) blocks")
      )

  implicit val validator: Validator[BlockSummaryV1] = schema.validator
}