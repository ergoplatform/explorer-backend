package org.ergoplatform.explorer.protocol.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.explorer.HexString

@derive(encoder, decoder)
final case class ExpandedRegister(
  rawValue: HexString,
  valueType: String,
  decodedValue: String
)
