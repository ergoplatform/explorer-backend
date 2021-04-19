package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.db.models.{Transaction, UTransaction}
import org.ergoplatform.explorer.protocol.constants
import sttp.tapir.{Schema, Validator}
import sttp.tapir.generic.Derived

final case class TxStats(totalCoinsTransferred: Long, totalFee: Long, feePerByte: Double)

object TxStats {

  implicit val codec: Codec[TxStats] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[TxStats] =
    Schema
      .derived[TxStats]
      .modify(_.totalCoinsTransferred)(
        _.description("Total amount of coins transferred by transaction")
      )
      .modify(_.totalFee)(
        _.description("Total amount of fee in the transaction")
      )
      .modify(_.feePerByte)(
        _.description("Amount of nanoERGs ber byte in transaction")
      )

  implicit val validator: Validator[TxStats] = schema.validator

  def apply(
    tx: Transaction,
    inputs: List[InputInfo],
    outputs: List[OutputInfo]
  ): TxStats = {
    val totalCoins = inputs.map(_.value.getOrElse(0L)).sum
    val totalFee = outputs
      .filter(_.ergoTree.unwrapped == constants.FeePropositionScriptHex)
      .map(_.value)
      .sum
    val feePerByte = if (tx.size == 0) 0d else totalFee.toDouble / tx.size
    TxStats(totalCoins, totalFee, feePerByte)
  }

  def apply(
    tx: UTransaction,
    inputs: List[UInputInfo],
    outputs: List[UOutputInfo]
  ): TxStats = {
    val totalCoins = inputs.map(_.value.getOrElse(0L)).sum
    val totalFee = outputs
      .filter(_.ergoTree.unwrapped == constants.FeePropositionScriptHex)
      .map(_.value)
      .sum
    val feePerByte = if (tx.size == 0) 0d else totalFee.toDouble / tx.size
    TxStats(totalCoins, totalFee, feePerByte)
  }
}
