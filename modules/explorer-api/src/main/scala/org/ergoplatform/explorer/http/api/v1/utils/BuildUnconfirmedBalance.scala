package org.ergoplatform.explorer.http.api.v1.utils

import org.ergoplatform.explorer.http.api.models.AssetInstanceInfo
import org.ergoplatform.explorer.http.api.v1.models._
import org.ergoplatform.explorer.{ErgoTree, HexString, TokenId}

object BuildUnconfirmedBalance {

  /** Build unconfirmed balance considering MemPool Data
    * by reducing a `List[`[[org.ergoplatform.explorer.http.api.v1.models.UTransactionInfo]]`]`
    * into a [[org.ergoplatform.explorer.http.api.v1.models.Balance]]
    *
    * Unconfirmed Balance arithmetic is completed in three steps:
    * <li> determine if a [[org.ergoplatform.explorer.http.api.v1.models.UTransactionInfo]] is a credit or debit by
    *  matching its [[org.ergoplatform.explorer.http.api.v1.models.UInputInfo]] to wallets </li>
    *  <li> reducing similar [[org.ergoplatform.explorer.http.api.v1.models.UOutputInfo]]
    *  sums (credits/debits) into a single value: `debitSum/creditSum` </li>
    *  <li> subtracting or adding `debitSum/creditSum` into iterations current balance </li>
    *
    * @param items  unsigned transactions from the MemPool
    * @param confirmedBalance last signed & unspent balance
    * @param ergoTree  for transaction type identification
    * @param hexString for NSum evaluation
    * @return
    */
  def apply(
    items: List[UTransactionInfo],
    confirmedBalance: Balance,
    ergoTree: ErgoTree,
    hexString: HexString
  ): Balance =
    items.foldLeft(confirmedBalance) { case (balance, transactionInfo) =>
      transactionInfo.inputs.head.ergoTree match {
        case ieT if ieT == ergoTree =>
          val debitSum = transactionInfo.outputs.foldLeft(0L) { case (sum, outputInfo) =>
            if (outputInfo.ergoTree != hexString) sum + outputInfo.value
            else sum
          }

          val debitSumTokenGroups = transactionInfo.outputs
            .foldLeft(Map[TokenId, AssetInstanceInfo]()) { case (groupedTokens, outputInfo) =>
              if (outputInfo.ergoTree != hexString)
                outputInfo.assets.foldLeft(groupedTokens) { case (gT, asset) =>
                  val gTAsset = gT.getOrElse(asset.tokenId, asset.copy(amount = 0L))
                  gT + (asset.tokenId -> gTAsset.copy(amount = gTAsset.amount + asset.amount))
                }
              else groupedTokens
            }

          val newTokensBalance = balance.tokens.map { token =>
            debitSumTokenGroups.get(token.tokenId).map { assetInfo =>
              token.copy(amount = token.amount - assetInfo.amount)
            } match {
              case Some(value) => value
              case None        => token
            }
          }

          Balance(balance.nanoErgs - debitSum, newTokensBalance)
        case _ =>
          val creditSum = transactionInfo.outputs.foldLeft(0L) { case (sum, outputInfo) =>
            if (outputInfo.ergoTree == hexString) sum + outputInfo.value
            else sum
          }

          val creditSumTokenGroups = transactionInfo.outputs
            .foldLeft(Map[TokenId, AssetInstanceInfo]()) { case (groupedTokens, outputInfo) =>
              if (outputInfo.ergoTree == hexString)
                outputInfo.assets.foldLeft(groupedTokens) { case (gT, asset) =>
                  val gTAsset = gT.getOrElse(asset.tokenId, asset.copy(amount = 0L))
                  gT + (asset.tokenId -> gTAsset.copy(amount = gTAsset.amount + asset.amount))
                }
              else groupedTokens
            }

          val newTokensBalance = balance.tokens.map { token =>
            creditSumTokenGroups.get(token.tokenId).map { assetInfo =>
              token.copy(amount = token.amount + assetInfo.amount)
            } match {
              case Some(value) => value
              case None        => token
            }
          }

          Balance(balance.nanoErgs + creditSum, newTokensBalance)
      }
    }
}
