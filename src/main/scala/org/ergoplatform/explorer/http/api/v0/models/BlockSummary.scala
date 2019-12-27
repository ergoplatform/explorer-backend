package org.ergoplatform.explorer.http.api.v0.models

import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class BlockSummary(info: FullBlockInfo, references: BlockReferencesInfo)

object BlockSummary {

  implicit val schema: Schema[BlockSummary] =
    implicitly[Derived[Schema[BlockSummary]]].value
      .modify(_.info)(_.description("Full block info"))
      .modify(_.references)(
        _.description("References to previous and next (if exists) blocks")
      )
}
