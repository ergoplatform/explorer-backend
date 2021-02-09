package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer._

final case class BoxRegister(
  id: RegisterId,
  boxId: BoxId,
  sigmaType: SigmaType,
  rawValue: HexString,
  renderedValue: String
)
