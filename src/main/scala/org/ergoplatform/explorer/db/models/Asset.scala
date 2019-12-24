package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.{AssetId, BoxId, Id}

/** Entity representing `node_assets` table.
  */
final case class Asset(
  id: AssetId,
  boxId: BoxId,
  headerId: Id,
  amount: Long
)
