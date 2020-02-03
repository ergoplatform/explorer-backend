package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.{BoxId, Id, TokenId}

/** Entity representing `node_assets` table.
  */
final case class Asset(
  tokenId: TokenId,
  boxId: BoxId,
  headerId: Id,
  amount: Long
)

object Asset {

  import schema.ctx._

  val quillSchemaMeta = schemaMeta[Asset](
    "node_assets",
    _.tokenId  -> "token_id",
    _.boxId    -> "box_id",
    _.headerId -> "header_id",
    _.amount   -> "value"
  )
}
