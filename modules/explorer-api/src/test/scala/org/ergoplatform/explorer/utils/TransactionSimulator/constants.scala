package org.ergoplatform.explorer.utils.TransactionSimulator

import org.ergoplatform.explorer.http.api.v1.models.{Balance, TokenAmount, UTransactionInfo}
import org.ergoplatform.explorer.{ErgoTree, HexString, TokenId, TokenType}

object constants {
  val TransactionFee = 1000000L // 0.001Erg
  val SigUSD1: HexString =
    HexString.fromStringUnsafe("514083a170fc734071c07748ff444940606654317bd516865120ed702952ab1b")

  val SigUSD2: HexString =
    HexString.fromStringUnsafe("be67102886d12d7302abea69838363eda5103709afdc66e935e774aedfad6e5d")

  val SenderAddressString   = "3WzSdM7NrjDJswpu2ThfhWvVM1mKJhgnGNieWYcGVsYp3AoirgR5"
  val ReceiverAddressString = "3Wx99DApJTpUTPZDhYEerbqWfa9MvuuVJehAFVeepnZMzAN3dfYW"
  val RandomAddressString   = "9iMUmLz4GZ7HfccBFPNxjQhN4Ysx1M9fJkZZH4keDVRXsejJd6n"

  val SenderHexString   = "0008cd03f9070bfe907cd5f9a5e06f907ebe8b002242f2f1ffb420cf2afab792253390bc"
  val ReceiverHexString = "0008cd02c9eee86f4ac1b55b655d9275cd71e5ab2e308fb847f8be19f47d59a0d369a91f"
  val RandomHexString   = ""

  val FeeAddressString =
    "2iHkR7CWvD1R4j1yXpSfpU37c6bRdeEaj3kcr5PsLfLRcPb3CCQwzQfkexKouu4WDTc4ybFSRWLsxqWS4U7sgbBkGgGZzXjbf5vxPXbx88eFwFzScV77g9vrUoXnPCqq1WgRvuqVMMuuyAoTWK7LSo"

  val FeeHexString =
    "1005040004000e36100204900108cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304"

  val SenderErgo: ErgoTree = ErgoTree(
    HexString.fromStringUnsafe(SenderHexString)
  )

  val ReceiverErgo: ErgoTree = ErgoTree(
    HexString.fromStringUnsafe(ReceiverHexString)
  )

  val FeeErgo: ErgoTree = ErgoTree(HexString.fromStringUnsafe(FeeHexString))

  case class WalletT(balance: Long, tokens: TestTokens)

  object WalletT {

    def toBalance(walletT: WalletT): Balance = {
      val tokens = walletT.tokens.toList.map { case (tId, value) =>
        val tokenInfo = TokenStoreT.getOrElse(
          tId,
          AssetInstanceInfoT(TokenId(SigUSD1), 2, Some("SigUSD1"), Some(2), Some(TokenType("EIP-004")))
        )

        TokenAmount(
          tokenInfo.tokenId,
          value,
          2,
          tokenInfo.name,
          tokenInfo.`type`
        )
      }
      Balance(walletT.balance, tokens)
    }
  }

  case class TransactionInfoT(ergoTree: ErgoTree, items: List[UTransactionInfo])
  case class SimulatedTransaction(sender: TransactionInfoT, receiver: TransactionInfoT)

  case class AssetInstanceInfoT(
    tokenId: TokenId,
    index: Int,
    name: Option[String],
    decimals: Option[Int],
    `type`: Option[TokenType]
  )

  type TestTokens = Map[TokenId, Long]

  val TokenStoreT: Map[TokenId, AssetInstanceInfoT] =
    Map(
      TokenId(SigUSD1) -> AssetInstanceInfoT(TokenId(SigUSD1), 0, Some("SigUSD1"), Some(2), Some(TokenType("EIP-004"))),
      TokenId(SigUSD2) -> AssetInstanceInfoT(TokenId(SigUSD2), 1, Some("SigUSD2"), Some(2), Some(TokenType("EIP-004")))
    )

}
