package org.ergoplatform.explorer.persistence.models

import io.circe.Json
import org.ergoplatform.explorer.{HexString, Id}

final case class BlockExtension(
  headerId: Id,
  digest: HexString,
  fields: Json
)
