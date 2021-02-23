package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.db.models.Transaction
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, ExtendedOutput, FullInput}
import org.ergoplatform.explorer.{Id, TxId}
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class TransactionInfo(
  id: TxId,
  blockId: Id,
  inclusionHeight: Int,
  timestamp: Long,
  index: Int,
  numConfirmations: Int,
  inputs: List[InputInfo],
  outputs: List[OutputInfo]
)

object TransactionInfo {

  implicit val codec: Codec[TransactionInfo] = deriveCodec

  implicit val validator: Validator[TransactionInfo] = Validator.derive

  implicit val schema: Schema[TransactionInfo] =
    Schema
      .derive[TransactionInfo]
      .modify(_.id)(_.description("Transaction ID"))
      .modify(_.blockId)(_.description("ID of the corresponding header"))
      .modify(_.inclusionHeight)(_.description("Height of the block the transaction was included in"))
      .modify(_.timestamp)(_.description("Timestamp the transaction got into the network"))
      .modify(_.index)(_.description("Index of a transaction inside a block"))
      .modify(_.numConfirmations)(_.description("Number of transaction confirmations"))

  def batch(
    txs: List[(Transaction, Int)],
    inputs: List[FullInput],
    outputs: List[ExtendedOutput],
    inAssets: List[ExtendedAsset],
    outAssets: List[ExtendedAsset]
  ): List[TransactionInfo] = {
    val groupedInAssets  = inAssets.groupBy(_.boxId)
    val groupedOutAssets = outAssets.groupBy(_.boxId)
    txs.map { case (tx, numConfirmations) =>
      val relatedInputs = inputs
        .filter(_.input.txId == tx.id)
        .sortBy(_.input.index)
        .map { in =>
          val relAssets = groupedInAssets.get(in.input.boxId).toList.flatten
          InputInfo(in, relAssets)
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
        numConfirmations,
        relatedInputs,
        relatedOutputs
      )
    }
  }
}
