package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.db.models.{UAsset, UTransaction}
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedUAsset, ExtendedUDataInput, ExtendedUInput, ExtendedUOutput}
import sttp.tapir.{Schema, Validator}
import sttp.tapir.generic.Derived

final case class UTransactionSummary(
  id: TxId,
  inputs: List[UInputInfo],
  dataInputs: List[UDataInputInfo],
  outputs: List[UOutputInfo],
  creationTimestamp: Long,
  size: Int,
  ioSummary: TxStats
)

object UTransactionSummary {

  implicit val codec: Codec[UTransactionSummary] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[UTransactionSummary] =
    Schema
      .derived[UTransactionSummary]
      .modify(_.id)(_.description("Transaction ID"))
      .modify(_.creationTimestamp)(
        _.description("Approximate time this transaction appeared in the network")
      )
      .modify(_.size)(
        _.description("Size of the transaction in bytes")
      )

  implicit val validator: Validator[UTransactionSummary] = schema.validator

  def apply(
             tx: UTransaction,
             ins: List[ExtendedUInput],
             dataIns: List[ExtendedUDataInput],
             outs: List[ExtendedUOutput],
             assets: List[ExtendedUAsset]
  ): UTransactionSummary = {
    val inputsInfo     = ins.map(UInputInfo.apply)
    val dataInputsInfo = dataIns.map(UDataInputInfo.apply)
    val outputsInfo    = UOutputInfo.batch(outs, assets)
    val stats          = TxStats(tx, inputsInfo, outputsInfo)
    new UTransactionSummary(tx.id, inputsInfo, dataInputsInfo, outputsInfo, tx.creationTimestamp, tx.size, stats)
  }
}
