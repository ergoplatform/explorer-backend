package org.ergoplatform.explorer.protocol.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.explorer.{HexString, SigmaType}

@derive(encoder, decoder)
final case class ExpandedRegister(
  serializedValue: HexString,
  sigmaType: Option[SigmaType],
  renderedValue: Option[String]
)

@derive(encoder, decoder)
final case class ExpandedLegacyRegister(
  rawValue: HexString,
  valueType: String,
  decodedValue: String
)
