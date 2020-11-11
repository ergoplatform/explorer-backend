package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.{BoxId, HexString, Id, RegisterId}

final case class BoxRegister(
  id: RegisterId,
  boxId: BoxId,
  headerId: Id,
  valueType: String,
  rawValue: HexString,
  decodedValue: String
)
