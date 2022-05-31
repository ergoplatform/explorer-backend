package org.ergoplatform.explorer.http.api.v1.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import sttp.tapir.{Schema, Validator}

final case class BlockSummaryV1(block: FullBlockInfoV1)

object BlockSummaryV1 {

  implicit val codec: Codec[BlockSummaryV1] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[BlockSummaryV1] =
    Schema
      .derived[BlockSummaryV1]
      .modify(_.block)(_.description("Full block info"))

  implicit val validator: Validator[BlockSummaryV1] = schema.validator
}
