package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.db.models.Transaction
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, ExtendedDataInput, ExtendedInput, ExtendedOutput}
import org.ergoplatform.explorer.{BlockId, TxId}
import sttp.tapir.{Schema, Validator}

final case class TransactionInfo(
  id: TxId,
  headerId: BlockId,
  inclusionHeight: Int,
  timestamp: Long,
  index: Int,
  confirmationsCount: Int,
  inputs: List[InputInfo],
  dataInputs: List[DataInputInfo],
  outputs: List[OutputInfo]
)

object TransactionInfo {

  implicit val codec: Codec[TransactionInfo] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[TransactionInfo] =
    Schema
      .derived[TransactionInfo]
      .modify(_.id)(_.description("Transaction ID"))
      .modify(_.headerId)(_.description("ID of the corresponding header"))
      .modify(_.inclusionHeight)(_.description("Height of the block the transaction was included in"))
      .modify(_.timestamp)(_.description("Timestamp the transaction got into the network"))
      .modify(_.index)(_.description("Index of a transaction inside a block"))
      .modify(_.confirmationsCount)(_.description("Number of transaction confirmations"))

  implicit val validator: Validator[TransactionInfo] = schema.validator

  def batch(
    txs: List[(Transaction, Int)],
    inputs: List[ExtendedInput],
    dataInputs: List[ExtendedDataInput],
    outputs: List[ExtendedOutput],
    assets: List[ExtendedAsset]
  ): List[TransactionInfo] = {
    val groupedAssets = assets.groupBy(_.boxId)
    txs.map { case (tx, numConfirmations) =>
      val relatedInputs = inputs
        .filter(_.input.txId == tx.id)
        .sortBy(_.input.index)
        .map(InputInfo.apply)
      val relatedDataInputs = dataInputs
        .filter(_.input.txId == tx.id)
        .sortBy(_.input.index)
        .map(DataInputInfo.apply)
      val relatedOutputs = outputs
        .filter(_.output.txId == tx.id)
        .sortBy(_.output.index)
        .map { out =>
          val relAssets = groupedAssets.get(out.output.boxId).toList.flatten
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
        relatedDataInputs,
        relatedOutputs
      )
    }
  }
}
