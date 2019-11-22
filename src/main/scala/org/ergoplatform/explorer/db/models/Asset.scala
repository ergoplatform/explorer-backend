package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.{AssetId, BoxId}

/** Entity representing `node_assets` table.
  */
final case class Asset(
  id: AssetId,
  boxId: BoxId,
  amount: Long
)
