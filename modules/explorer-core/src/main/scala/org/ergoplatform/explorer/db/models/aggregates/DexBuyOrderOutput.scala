package org.ergoplatform.explorer.db.models.aggregates

import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.db.repositories.DexOrdersRepo.TokenInfo

final case class DexBuyOrderOutput(
  extOutput: ExtendedOutput,
  tokenId: TokenId,
  tokenAmount: Long
)

object DexBuyOrderOutput {

  def apply(extOutput: ExtendedOutput, tokenInfo: TokenInfo): DexBuyOrderOutput =
    new DexBuyOrderOutput(extOutput, tokenInfo.tokenId, tokenInfo.amount)
}
