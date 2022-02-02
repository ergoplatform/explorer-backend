package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.explorer
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.db.models.UTransaction
import org.ergoplatform.explorer.db.models.aggregates._
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class UTransactionInfo(
  id: TxId,
  creationTimestamp: Long,
  inputs: List[UInputInfo],
  dataInputs: List[UDataInputInfo],
  outputs: List[UOutputInfo],
  size: Int
)

object UTransactionInfo {

  implicit val schema: Schema[UTransactionInfo] =
    Schema
      .derived[UTransactionInfo]
      .modify(_.id)(_.description("Transaction ID"))
      .modify(_.creationTimestamp)(_.description("Timestamp the transaction got into the network"))
      .modify(_.size)(_.description("Transaction size in bytes"))

  implicit val validator: Validator[UTransactionInfo] = schema.validator

  def unFlatten(
    tx: UTransaction,
    inputs: List[ExtendedUInput],
    dataInputs: List[ExtendedUDataInput],
    outputs: List[ExtendedUOutput],
    inAssets: List[ExtendedUAsset],
    confInAssets: List[ExtendedAsset],
    outAssets: List[ExtendedUAsset]
  ): UTransactionInfo = {
    val groupedInAssets     = inAssets.groupBy(_.boxId)
    val groupedConfInAssets = confInAssets.groupBy(_.boxId)
    val groupedOutAssets    = outAssets.groupBy(_.boxId)
    unFlattenIn(tx, inputs, dataInputs, outputs, groupedInAssets, groupedConfInAssets, groupedOutAssets)
  }

  def unFlattenBatch(
    txs: List[UTransaction],
    inputs: List[ExtendedUInput],
    dataInputs: List[ExtendedUDataInput],
    outputs: List[ExtendedUOutput],
    inAssets: List[ExtendedUAsset],
    confInAssets: List[ExtendedAsset],
    outAssets: List[ExtendedUAsset]
  ): List[UTransactionInfo] = {
    val groupedInAssets     = inAssets.groupBy(_.boxId)
    val groupedConfInAssets = confInAssets.groupBy(_.boxId)
    val groupedOutAssets    = outAssets.groupBy(_.boxId)
    txs.map(unFlattenIn(_, inputs, dataInputs, outputs, groupedInAssets, groupedConfInAssets, groupedOutAssets))
  }

  private def unFlattenIn(
    tx: UTransaction,
    inputs: List[ExtendedUInput],
    dataInputs: List[ExtendedUDataInput],
    outputs: List[ExtendedUOutput],
    groupedInAssets: Map[explorer.BoxId, List[ExtendedUAsset]],
    groupedConfInAssets: Map[explorer.BoxId, List[ExtendedAsset]],
    groupedOutAssets: Map[explorer.BoxId, List[ExtendedUAsset]]
  ): UTransactionInfo = {
    val relatedInputs = inputs
      .filter(_.input.txId == tx.id)
      .sortBy(_.input.index)
      .map { in =>
        val relAssets     = groupedInAssets.get(in.input.boxId).toList.flatten
        val relConfAssets = groupedConfInAssets.get(in.input.boxId).toList.flatten
        UInputInfo(in, relAssets, relConfAssets)
      }
    val relatedDataInputs = dataInputs
      .filter(_.input.txId == tx.id)
      .sortBy(_.input.index)
      .map { in =>
        val relAssets = groupedInAssets.get(in.input.boxId).toList.flatten
        UDataInputInfo(in, relAssets)
      }
    val relatedOutputs = outputs
      .filter(_.output.txId == tx.id)
      .sortBy(_.output.index)
      .map { out =>
        val relAssets = groupedOutAssets.get(out.output.boxId).toList.flatten
        UOutputInfo(out, relAssets)
      }
    apply(
      tx.id,
      tx.creationTimestamp,
      relatedInputs,
      relatedDataInputs,
      relatedOutputs,
      tx.size
    )
  }
}
