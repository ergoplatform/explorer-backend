package org.ergoplatform.explorer.utils.TransactionSimulator

import org.ergoplatform.explorer.http.api.models.AssetInstanceInfo
import org.ergoplatform.explorer.http.api.v1.models.{UOutputInfo, UTransactionInfo}
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.utils.TransactionSimulator.constants._

import java.util.UUID.randomUUID
import scala.util.Try

/** Simulate Ergo Transfer between two wallets: <br>
  * - Generate Input & OutputBoxes
  */
object Simulator {

  def apply(
    sender: WalletT,
    receiver: WalletT,
    amountToSend: Long,
    tokensToSend: Option[Map[TokenId, Long]]
  ): Try[SimulatedTransaction] = Try {

    if (tokensToSend.isDefined)
      require(
        sender.tokens.toList
          .map(_._1) == tokensToSend.map(_.toList.map(_._1)).getOrElse(List()),
        "tokens being sent must be available in senders wallet"
      )

    require(sender.balance >= (amountToSend + TransactionFee), "ergo - increase sending wallet balance")

    val senderAddress   = Address.fromString[Try](SenderAddressString).get
    val receiverAddress = Address.fromString[Try](ReceiverAddressString).get
    val feeAddress      = Address.fromString[Try](FeeAddressString).get

    val sInputBoxes = List(UInputGen(sender, SenderErgo, senderAddress, TokenStoreT))
    val rInputBoxes = List(UInputGen(receiver, ReceiverErgo, receiverAddress, TokenStoreT))

    val tFeeOutput = UOutputGen(
      TransactionFee,
      FeeErgo.value,
      feeAddress,
      List()
    )

    // For sender: debit output box + fee
    val sentAssets: List[AssetInstanceInfo] =
      tokensToSend
        .map(_.toList.map { case (tId, amountToSend) =>
          val tokenInfo = TokenStoreT.getOrElse(
            tId,
            AssetInstanceInfoT(TokenId(SigUSD1), 2, Some("SigUSD1"), Some(2), Some(TokenType("EIP-004")))
          )
          AssetInstanceInfo(
            tokenInfo.tokenId,
            tokenInfo.index,
            amountToSend,
            tokenInfo.name,
            tokenInfo.decimals,
            tokenInfo.`type`
          )

        })
        .getOrElse(List[AssetInstanceInfo]())

    val outputBoxes: List[UOutputInfo] =
      UOutputGen(amountToSend, ReceiverErgo.value, receiverAddress, sentAssets) :: List()

    val sTransactionInfoT = TransactionInfoT(
      SenderErgo,
      UTransactionInfo(
        TxId(randomUUID().toString),
        System.currentTimeMillis(),
        sInputBoxes,
        List(),
        tFeeOutput :: outputBoxes,
        3
      ) :: List()
    )

    val rTransactionInfo = TransactionInfoT(
      ReceiverErgo,
      UTransactionInfo(
        TxId(randomUUID().toString),
        System.currentTimeMillis(),
        sInputBoxes,
        List(),
        outputBoxes,
        3
      ) :: List()
    )

    SimulatedTransaction(sTransactionInfoT, rTransactionInfo)
  }

}
