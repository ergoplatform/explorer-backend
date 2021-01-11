package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.{BoxId, TokenId, TokenType}

final case class Token(
  id: TokenId,
  boxId: BoxId,
  name: String,
  description: String,
  `type`: TokenType,
  decimals: Int,
  emissionAmount: Long
)
