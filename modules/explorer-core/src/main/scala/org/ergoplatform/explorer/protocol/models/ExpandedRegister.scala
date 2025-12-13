package org.ergoplatform.explorer.protocol.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.explorer.{HexString, SigmaType}
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class ExpandedRegister(
  serializedValue: HexString,
  sigmaType: Option[SigmaType],
  renderedValue: Option[String]
)

object ExpandedRegister {
  implicit val schema: Schema[ExpandedRegister] =
    Schema
      .derived[ExpandedRegister]
      .modify(_.serializedValue)(_.description("Serialized register value in hex"))
      .modify(_.sigmaType)(_.description("Sigma type of the register value"))
      .modify(_.renderedValue)(_.description("Human-readable rendered value"))

  implicit val validator: Validator[ExpandedRegister] = schema.validator
}

@derive(encoder, decoder)
final case class ExpandedLegacyRegister(
  rawValue: HexString,
  valueType: String,
  decodedValue: String
)
