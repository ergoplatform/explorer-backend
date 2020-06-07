package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.db.models.{UAsset, UOutput, UTransaction}
import org.ergoplatform.explorer.db.models.aggregates.ExtendedUInput
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class UTransactionSummary(
  id: TxId,
  inputs: List[UInputInfo],
  outputs: List[UOutputInfo],
  creationTimestamp: Long,
  size: Int,
  ioSummary: TxStats
)

object UTransactionSummary {

  implicit val codec: Codec[UTransactionSummary] = deriveCodec

  implicit val schema: Schema[UTransactionSummary] =
    implicitly[Derived[Schema[UTransactionSummary]]].value
      .modify(_.id)(_.description("Transaction ID"))
      .modify(_.creationTimestamp)(
        _.description("Approximate time this transaction appeared in the network")
      )
      .modify(_.size)(
        _.description("Size of the transaction in bytes")
      )

  def apply(
    tx: UTransaction,
    ins: List[ExtendedUInput],
    outs: List[UOutput],
    assets: List[UAsset]
  ): UTransactionSummary = {
    val inputsInfo  = ins.map(UInputInfo.apply)
    val outputsInfo = UOutputInfo.batch(outs, assets)
    val stats       = TxStats(tx, inputsInfo, outputsInfo)
    new UTransactionSummary(tx.id, inputsInfo, outputsInfo, tx.creationTimestamp, tx.size, stats)
  }
}
