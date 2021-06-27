package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.explorer.{ErgoTreeTemplateHash, TokenId}
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class BoxAssetsQuery(
  ergoTreeTemplateHash: ErgoTreeTemplateHash,
  assets: List[TokenId]
)

object BoxAssetsQuery {

  implicit val schema: Schema[BoxAssetsQuery] =
    Schema
      .derived[BoxAssetsQuery]
      .modify(_.ergoTreeTemplateHash)(_.description("SHA-256 hash of ErgoTree template this box script should have"))
      .modify(_.assets)(_.description("IDs of tokens returned boxes should contain"))

  implicit val validator: Validator[BoxAssetsQuery] = Validator.pass
}
