package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.{BoxId, TokenId}

/** Represents `node_u_assets` table (Unconfirmed Asset, which is a part of Unconfirmed Transaction).
  */
final case class UAsset(
  tokenId: TokenId,
  boxId: BoxId,
  index: Int,
  amount: Long
)
