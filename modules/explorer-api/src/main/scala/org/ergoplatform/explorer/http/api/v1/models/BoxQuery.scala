package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.explorer.{ErgoTreeTemplateHash, RegisterId, TokenId}
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class BoxQuery(
  ergoTreeTemplateHash: ErgoTreeTemplateHash,
  registers: Option[Map[RegisterId, String]],
  constants: Option[Map[Int, String]],
  assets: Option[List[TokenId]]
)

object BoxQuery {

  implicit val schema: Schema[BoxQuery] =
    Schema
      .derive[BoxQuery]
      .modify(_.ergoTreeTemplateHash)(_.description("SHA-256 hash of ErgoTree template this box script should have"))
      .modify(_.registers)(_.description("Pairs of (register ID, register value) this box should contain"))
      .modify(_.constants)(_.description("Pairs of (constant index, constant value) this box should contain"))
      .modify(_.constants)(_.description("IDs of tokens this box should contain"))

  implicit val validator: Validator[BoxQuery] = Validator.pass
}
