package org.ergoplatform.explorer.db.models

import io.circe.Json
import org.ergoplatform.explorer.{HexString, Id}

/** Entity representing `node_extensions` table.
 */
final case class BlockExtension(
  headerId: Id,
  digest: HexString,
  fields: Json
)
