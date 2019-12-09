package org.ergoplatform.explorer.db.models

import io.circe.Json
import org.ergoplatform.explorer.protocol.models.ApiBlockExtension
import org.ergoplatform.explorer.{HexString, Id}

/** Entity representing `node_extensions` table.
 */
final case class BlockExtension(
  headerId: Id,
  digest: HexString,
  fields: Json       // arbitrary key->value dictionary
)

object BlockExtension {

  def fromApi(apiExtension: ApiBlockExtension): BlockExtension =
    BlockExtension(
      apiExtension.headerId,
      apiExtension.digest,
      apiExtension.fields
    )
}
