package org.ergoplatform.explorer.v1.utils

import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.http.api.v1.utils.BuildUnconfirmedBalance
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.ergoplatform.explorer.utils.TransactionSimulator.{constants, Simulator}
import org.ergoplatform.explorer.utils.TransactionSimulator.constants.{SigUSD1, SigUSD2, TransactionFee, WalletT}

import scala.util.{Success, Try}

class BuildUnconfirmedBalanceSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  // 100Erg 100Token
  val senderWallet: WalletT = WalletT(
    100000000000L,
    Map(
      TokenId(SigUSD1) -> 10000L,
      TokenId(SigUSD2) -> 10000L
    )
  )

  // 10Erg 10
  val receivingWallet: WalletT = WalletT(
    100000000000L,
    Map(
      TokenId(SigUSD1) -> 10000L,
      TokenId(SigUSD2) -> 10000L
    )
  )

  property("calculate sending wallet nanoErg unconfirmed balance") {
    val toSend = 5000000000L // 5Erg
    val res    = Simulator(senderWallet, receivingWallet, toSend, None)
    res shouldBe a[Success[_]]

    val txInfo = res.get.sender

    val unconfirmedBalance =
      BuildUnconfirmedBalance(txInfo.items, WalletT.toBalance(senderWallet), txInfo.ergoTree, txInfo.ergoTree.value)

    unconfirmedBalance.nanoErgs shouldBe (senderWallet.balance - TransactionFee - toSend)
  }

  property("calculate receiving wallet nanoErg unconfirmed balance") {
    val toSend = 8000000000L // 8Erg
    val res    = Simulator(senderWallet, receivingWallet, toSend, None)
    res shouldBe a[Success[_]]

    val txInfo = res.get.receiver

    val unconfirmedBalance =
      BuildUnconfirmedBalance(txInfo.items, WalletT.toBalance(receivingWallet), txInfo.ergoTree, txInfo.ergoTree.value)

    unconfirmedBalance.nanoErgs shouldBe (receivingWallet.balance + toSend)
  }

  property("calculate sending wallet assets unconfirmed balance") {
    val tokenSentAmount = 100L
    val res = Simulator(
      senderWallet,
      receivingWallet,
      TransactionFee,
      Some(
        Map(
          TokenId(SigUSD1) -> tokenSentAmount,
          TokenId(SigUSD2) -> tokenSentAmount
        )
      )
    )
    res shouldBe a[Success[_]]

    val txInfo           = res.get.sender
    val sWalletAsBalance = WalletT.toBalance(senderWallet)

    val unconfirmedBalance =
      BuildUnconfirmedBalance(txInfo.items, sWalletAsBalance, txInfo.ergoTree, txInfo.ergoTree.value)

    unconfirmedBalance.tokens shouldBe sWalletAsBalance.tokens.map(t => t.copy(amount = t.amount - tokenSentAmount))

  }

  property("calculate receiving wallet assets unconfirmed balance") {
    val tokenSentAmount = 100L
    val res = Simulator(
      senderWallet,
      receivingWallet,
      TransactionFee,
      Some(
        Map(
          TokenId(SigUSD1) -> tokenSentAmount,
          TokenId(SigUSD2) -> tokenSentAmount
        )
      )
    )
    res shouldBe a[Success[_]]

    val txInfo           = res.get.receiver
    val rWalletAsBalance = WalletT.toBalance(receivingWallet)

    val unconfirmedBalance =
      BuildUnconfirmedBalance(txInfo.items, rWalletAsBalance, txInfo.ergoTree, txInfo.ergoTree.value)

    unconfirmedBalance.tokens shouldBe rWalletAsBalance.tokens.map(t => t.copy(amount = t.amount + tokenSentAmount))
  }
}
