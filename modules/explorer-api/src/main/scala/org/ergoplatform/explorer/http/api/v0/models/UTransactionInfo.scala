package org.ergoplatform.explorer.http.api.v0.models

import org.ergoplatform.explorer.db.models.{UAsset, UInput, UOutput, UTransaction}

final case class UTransactionInfo(
  id: String,
  inputs: List[UInputInfo],
  outputs: List[UOutputInfo],
  creationTimestamp: Long,
  size: Int
)

object UTransactionInfo {

  def apply(
    tx: UTransaction,
    ins: List[UInput],
    outs: List[UOutput],
    assets: List[UAsset]
  ): UTransactionInfo = ???

  def batch(
    txs: List[UTransaction],
    ins: List[UInput],
    outs: List[UOutput],
    assets: List[UAsset]
  ): List[UTransactionInfo] = ???
}
