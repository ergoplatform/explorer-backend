package org.ergoplatform.explorer.http.api.v0.models

import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import io.circe.syntax._
import io.circe.{Codec, Decoder, Encoder, Json}
import org.ergoplatform.explorer.db.models.Transaction
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, ExtendedDataInput, ExtendedInput, ExtendedOutput}
import org.ergoplatform.explorer.http.api.v0.models.TransactionSummary.MiniBlockInfo
import org.ergoplatform.explorer.{BlockId, TxId}
import sttp.tapir.{Schema, Validator}

final case class TransactionSummary(
  id: TxId,
  miniBlockInfo: MiniBlockInfo,
  timestamp: Long,
  index: Int,
  confirmationsCount: Int,
  inputs: List[InputInfo],
  dataInputs: List[DataInputInfo],
  outputs: List[OutputInfo],
  size: Int,
  ioSummary: TxStats
)

object TransactionSummary {

  final case class MiniBlockInfo(id: BlockId, height: Int)

  implicit val decoder: Decoder[TransactionSummary] = deriveMagnoliaDecoder

  implicit val encoder: Encoder[TransactionSummary] = { ts =>
    Json.obj(
      "summary" -> Json.obj(
        "id"                 -> ts.id.asJson,
        "timestamp"          -> ts.timestamp.asJson,
        "index"              -> ts.index.asJson,
        "size"               -> ts.size.asJson,
        "confirmationsCount" -> ts.confirmationsCount.asJson,
        "block"              -> ts.miniBlockInfo.asJson
      ),
      "ioSummary"  -> ts.ioSummary.asJson,
      "inputs"     -> ts.inputs.asJson,
      "dataInputs" -> ts.dataInputs.asJson,
      "outputs"    -> ts.outputs.asJson
    )
  }

  implicit def codec: Codec[TransactionSummary] = Codec.from(decoder, encoder)

  implicit def codecBlockInfo: Codec[MiniBlockInfo] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schemaBlockInfo: Schema[MiniBlockInfo] =
    Schema
      .derived[MiniBlockInfo]
      .modify(_.id)(_.description("Block ID"))
      .modify(_.height)(_.description("Block height"))

  implicit val validatorBlockInfo: Validator[MiniBlockInfo] = schemaBlockInfo.validator

  implicit val schema: Schema[TransactionSummary] =
    Schema
      .derived[TransactionSummary]
      .modify(_.id)(_.description("Transaction ID"))
      .modify(_.timestamp)(_.description("Timestamp the transaction got into the network"))
      .modify(_.index)(_.description("Index of a transaction inside a block"))
      .modify(_.confirmationsCount)(_.description("Number of transaction confirmations"))
      .modify(_.size)(_.description("Size of a transaction in bytes"))

  implicit val validator: Validator[TransactionSummary] = schema.validator

  def apply(
    tx: Transaction,
    numConfirmations: Int,
    inputs: List[ExtendedInput],
    dataInputs: List[ExtendedDataInput],
    outputs: List[ExtendedOutput],
    assets: List[ExtendedAsset]
  ): TransactionSummary = {
    val ins     = InputInfo.batch(inputs)
    val dataIns = DataInputInfo.batch(dataInputs)
    val outs    = OutputInfo.batch(outputs, assets)
    apply(tx, numConfirmations, ins, dataIns, outs)
  }

  private def apply(
    tx: Transaction,
    numConfirmations: Int,
    inputs: List[InputInfo],
    dataInputs: List[DataInputInfo],
    outputs: List[OutputInfo]
  ): TransactionSummary = {
    val stats     = TxStats(tx, inputs, outputs)
    val blockInfo = MiniBlockInfo(tx.headerId, tx.inclusionHeight)
    apply(
      tx.id,
      blockInfo,
      tx.timestamp,
      tx.index,
      numConfirmations,
      inputs,
      dataInputs,
      outputs,
      tx.size,
      stats
    )
  }
}
