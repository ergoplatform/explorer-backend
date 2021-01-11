package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.{TokenId, TokenType}

final case class Token(
  id: TokenId,
  name: String,
  description: String,
  `type`: TokenType,
  decimals: Int,
  emissionAmount: Long
)
