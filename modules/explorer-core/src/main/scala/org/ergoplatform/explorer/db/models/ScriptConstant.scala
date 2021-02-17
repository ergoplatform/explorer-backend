package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.{BoxId, HexString, SigmaType}

final case class ScriptConstant(
  index: Int,
  boxId: BoxId,
  sigmaType: SigmaType,
  serializedValue: HexString,
  renderedValue: String
)
