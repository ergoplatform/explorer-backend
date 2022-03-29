package org.ergoplatform.explorer.utils

import org.ergoplatform.explorer.http.api.v1.models.UTransactionInfo
import org.ergoplatform.explorer.{ErgoTree, HexString, TokenId, TokenType}

import scala.util.Try

/** Simulate Money Transfer between two wallets
  */
object TransactionSimulator {
  case class TestWallet(balance: Long, tokens: TestTokens)
  case class TransactionInfo(ergoTree: ErgoTree, items: List[UTransactionInfo])
  case class SimulatedTransaction(sender: TransactionInfo, receiver: TransactionInfo)

  case class AssetInstanceInfoT(
    tokenId: TokenId,
    name: Option[String],
    decimals: Option[Int],
    `type`: Option[TokenType]
  )

  type TestTokens = Map[TokenId, Long]

  val TransactionFee = 1000000L // 0.001Erg
  val SigUSD1: HexString =
    HexString.fromStringUnsafe("514083a170fc734071c07748ff444940606654317bd516865120ed702952ab1b")

  val SigUSD2: HexString =
    HexString.fromStringUnsafe("be67102886d12d7302abea69838363eda5103709afdc66e935e774aedfad6e5d")

  val AvailableTestTokens: Map[TokenId, AssetInstanceInfoT] =
    Map(
      TokenId(SigUSD1) -> AssetInstanceInfoT(TokenId(SigUSD1), Some("SigUSD1"), Some(2), Some(TokenType("EIP-004"))),
      TokenId(SigUSD2) -> AssetInstanceInfoT(TokenId(SigUSD2), Some("SigUSD2"), Some(2), Some(TokenType("EIP-004")))
    )

  def apply(
    sender: TestWallet,
    receiver: TestWallet,
    amountToSend: Option[Long],
    tokensToSend: Option[Map[TokenId, Long]]
  ): Try[SimulatedTransaction] = {
    require(amountToSend.nonEmpty || tokensToSend.nonEmpty, "provide ergo/token value to simulate transaction")
    // amountToSend and token to send must be less than balance + fee

    ???
  }

  // generate Input boxes for sender & receiver wallets
  // generate Output boxes matching money movement

}
