package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.TokenId

final case class GenuineToken(
  id: TokenId,
  tokenName: String,
  uniqueName: Boolean,
  issuer: Option[String]
)
