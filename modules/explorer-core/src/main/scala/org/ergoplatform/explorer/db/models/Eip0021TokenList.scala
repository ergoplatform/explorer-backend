package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.protocol.models.GenuineTokens

final case class Eip0021TokenList(genuineTokens: List[GenuineTokens], blockedTokens: List[TokenId])
