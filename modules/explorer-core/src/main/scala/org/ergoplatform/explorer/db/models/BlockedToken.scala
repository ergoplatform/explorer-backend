package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.TokenId

final case class BlockedToken(
  tokenId: TokenId,
  tokenName: String
)
