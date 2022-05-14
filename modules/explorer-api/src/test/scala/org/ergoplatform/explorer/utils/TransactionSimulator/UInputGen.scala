package org.ergoplatform.explorer.utils.TransactionSimulator

import io.circe.Json
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.http.api.models.AssetInstanceInfo
import org.ergoplatform.explorer.http.api.v1.models.UInputInfo
import org.ergoplatform.explorer.utils.TransactionSimulator.constants._

import java.util.UUID.randomUUID

object UInputGen {

  def apply(
    wallet: WalletT,
    ergoTree: ErgoTree,
    address: Address,
    availableTestTokens: Map[TokenId, AssetInstanceInfoT]
  ): UInputInfo = UInputInfo(
    BoxId(randomUUID().toString),
    wallet.balance,
    0,
    None,
    None,
    TxId(randomUUID().toString),
    0,
    ergoTree,
    address,
    wallet.tokens.toList.map { case (tId, value) =>
      val tokenInfo = availableTestTokens.getOrElse(
        tId,
        AssetInstanceInfoT(TokenId(SigUSD1), 2, Some("SigUSD1"), Some(2), Some(TokenType("EIP-004")))
      )

      AssetInstanceInfo(
        tokenInfo.tokenId,
        tokenInfo.index,
        value,
        tokenInfo.name,
        tokenInfo.decimals,
        tokenInfo.`type`
      )
    },
    Json.Null
  )
}
