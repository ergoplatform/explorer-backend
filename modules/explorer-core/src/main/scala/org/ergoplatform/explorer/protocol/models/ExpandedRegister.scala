package org.ergoplatform.explorer.protocol.models

import derevo.circe.encoder
import derevo.derive
import org.ergoplatform.explorer.HexString

@derive(encoder)
final case class ExpandedRegister(
  rawValue: HexString,
  valueType: String,
  decodedValue: String
)
