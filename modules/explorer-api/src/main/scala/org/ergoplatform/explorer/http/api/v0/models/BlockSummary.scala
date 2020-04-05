package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class BlockSummary(block: FullBlockInfo, references: BlockReferencesInfo)

object BlockSummary {

  implicit val codec: Codec[BlockSummary] = deriveCodec

  implicit val schema: Schema[BlockSummary] =
    implicitly[Derived[Schema[BlockSummary]]].value
      .modify(_.block)(_.description("Full block info"))
      .modify(_.references)(
        _.description("References to previous and next (if exists) blocks")
      )
}
