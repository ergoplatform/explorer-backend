package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.explorer
import org.ergoplatform.explorer.db.models.Transaction
import org.ergoplatform.explorer.db.models.aggregates._
import org.ergoplatform.explorer.{BlockId, TxId}
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class TransactionInfo(
  id: TxId,
  blockId: BlockId,
  inclusionHeight: Int,
  timestamp: Long,
  index: Int,
  globalIndex: Long,
  numConfirmations: Int,
  inputs: List[InputInfo],
  dataInputs: List[DataInputInfo],
  outputs: List[OutputInfo],
  size: Int
)

object TransactionInfo {

  implicit val schema: Schema[TransactionInfo] =
    Schema
      .derived[TransactionInfo]
      .modify(_.id)(_.description("Transaction ID"))
      .modify(_.blockId)(_.description("ID of the corresponding header"))
      .modify(_.inclusionHeight)(_.description("Height of the block the transaction was included in"))
      .modify(_.timestamp)(_.description("Timestamp the transaction got into the network"))
      .modify(_.index)(_.description("Index of a transaction inside a block"))
      .modify(_.globalIndex)(_.description("global index of a transaction in the blockchain"))
      .modify(_.numConfirmations)(_.description("Number of transaction confirmations"))
      .modify(_.size)(_.description("Transaction size in bytes"))

  implicit val validator: Validator[TransactionInfo] = schema.validator

  def unFlatten(
    tx: Transaction,
    numConfirmations: Int,
    inputs: List[FullInput],
    dataInputs: List[FullDataInput],
    outputs: List[ExtendedOutput],
    inAssets: List[ExtendedAsset],
    outAssets: List[ExtendedAsset]
  ): TransactionInfo = {
    val groupedInAssets  = inAssets.groupBy(_.boxId)
    val groupedOutAssets = outAssets.groupBy(_.boxId)
    unFlattenIn(tx, numConfirmations, inputs, dataInputs, outputs, groupedInAssets, groupedOutAssets)
  }

  def unFlattenBatch(
    txs: List[(Transaction, Int)],
    inputs: List[FullInput],
    dataInputs: List[FullDataInput],
    outputs: List[ExtendedOutput],
    inAssets: List[ExtendedAsset],
    outAssets: List[ExtendedAsset]
  ): List[TransactionInfo] = {
    val groupedInAssets  = inAssets.groupBy(_.boxId)
    val groupedOutAssets = outAssets.groupBy(_.boxId)
    txs.map { case (tx, numConfirmations) =>
      unFlattenIn(tx, numConfirmations, inputs, dataInputs, outputs, groupedInAssets, groupedOutAssets)
    }
  }

  private def unFlattenIn(
    tx: Transaction,
    numConfirmations: Int,
    inputs: List[FullInput],
    dataInputs: List[FullDataInput],
    outputs: List[ExtendedOutput],
    groupedInAssets: Map[explorer.BoxId, List[ExtendedAsset]],
    groupedOutAssets: Map[explorer.BoxId, List[ExtendedAsset]]
  ): TransactionInfo = {
    val relatedInputs = inputs
      .filter(_.input.txId == tx.id)
      .sortBy(_.input.index)
      .map { in =>
        val relAssets = groupedInAssets.get(in.input.boxId).toList.flatten
        InputInfo(in, relAssets)
      }
    val relatedDataInputs = dataInputs
      .filter(_.input.txId == tx.id)
      .sortBy(_.input.index)
      .map { in =>
        val relAssets = groupedInAssets.get(in.input.boxId).toList.flatten
        DataInputInfo(in, relAssets)
      }
    val relatedOutputs = outputs
      .filter(_.output.txId == tx.id)
      .sortBy(_.output.index)
      .map { out =>
        val relAssets = groupedOutAssets.get(out.output.boxId).toList.flatten
        OutputInfo(out, relAssets)
      }
    apply(
      tx.id,
      tx.headerId,
      tx.inclusionHeight,
      tx.timestamp,
      tx.index,
      tx.globalIndex,
      numConfirmations,
      relatedInputs,
      relatedDataInputs,
      relatedOutputs,
      tx.size
    )
  }
}
