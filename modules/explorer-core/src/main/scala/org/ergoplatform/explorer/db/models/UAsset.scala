package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.{BoxId, TokenId}

/** Entity representing `node_u_assets` table.
  */
final case class UAsset(
  tokenId: TokenId,
  boxId: BoxId,
  amount: Long
)
