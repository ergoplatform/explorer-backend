package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedInput, ExtendedOutput}
import org.ergoplatform.explorer.db.models.{Asset, Transaction}
import org.ergoplatform.explorer.protocol.constants
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class TransactionInfo(
  id: TxId,
  miniBlockInfo: MiniBlockInfo,
  timestamp: Long,
  confirmationsCount: Int,
  inputs: List[InputInfo],
  outputs: List[OutputInfo],
  size: Int,
  totalCoins: Long,
  totalFee: Long,
  feePerByte: Double
)

object TransactionInfo {

  implicit val codec: Codec[TransactionInfo] = deriveCodec

  implicit val schema: Schema[TransactionInfo] =
    implicitly[Derived[Schema[TransactionInfo]]].value
      .modify(_.id)(_.description("Transaction ID"))
      .modify(_.timestamp)(
        _.description("Timestamp the transaction got into the network")
      )
      .modify(_.confirmationsCount)(_.description("Number of transaction confirmations"))
      .modify(_.size)(_.description("Size of transaction in bytes"))
      .modify(_.totalCoins)(_.description("Total amount of nanoErgs in transaction"))
      .modify(_.totalFee)(_.description("Total amount of fee in transaction in nanoErgs"))
      .modify(_.feePerByte)(_.description("Amount of nanoErgs paid for each byte of transaction"))

  def batch(
    txs: List[(Transaction, Int)],
    inputs: List[ExtendedInput],
    outputs: List[ExtendedOutput],
    assets: List[Asset]
  ): List[TransactionInfo] = {
    val groupedAssets = assets.groupBy(_.boxId)
    txs.map {
      case (tx, numConfirmations) =>
        val relatedInputs = inputs
          .filter(_.input.txId == tx.id)
          .map(InputInfo.apply)
        val relatedOutputs = outputs
          .filter(_.output.txId == tx.id)
          .map { out =>
            OutputInfo(out, groupedAssets.get(out.output.boxId).toList.flatten)
          }
        apply(tx, numConfirmations, relatedInputs, relatedOutputs)
    }
  }

  def apply(
    tx: Transaction,
    numConfirmations: Int,
    inputs: List[ExtendedInput],
    outputs: List[ExtendedOutput],
    assets: List[Asset]
  ): TransactionInfo = {
    val ins  = inputs.map(InputInfo.apply)
    val outs = OutputInfo.batch(outputs, assets)
    apply(tx, numConfirmations, ins, outs)
  }

  private def apply(
    tx: Transaction,
    numConfirmations: Int,
    inputs: List[InputInfo],
    outputs: List[OutputInfo]
  ): TransactionInfo = {
    val TxStats(totalCoins, totalFee, feePerByte) = txStats(tx, inputs, outputs)
    val blockInfo                                 = MiniBlockInfo(tx.headerId, tx.inclusionHeight)
    apply(
      tx.id,
      blockInfo,
      tx.timestamp,
      numConfirmations,
      inputs,
      outputs,
      tx.size,
      totalCoins,
      totalFee,
      feePerByte
    )
  }

  private def txStats(
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

  final private case class TxStats(totalCoins: Long, totalFee: Long, feePerByte: Double)
}
