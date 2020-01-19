package org.ergoplatform.explorer.http.api.v0.models

import org.ergoplatform.explorer.{BoxId, TxId}
import org.ergoplatform.explorer.db.models.UInput

final case class UInputInfo(boxId: BoxId, txId: TxId, spendingProof: SpendingProofInfo)

object UInputInfo {

  def apply(in: UInput): UInputInfo =
    UInputInfo(in.boxId, in.txId, SpendingProofInfo(in.proofBytes, in.extension))
}
