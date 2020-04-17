package org.ergoplatform.explorer.http.api.v0.models

import io.circe.{Codec, Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder}
import io.circe.syntax._
import org.ergoplatform.explorer.{Id, TxId}
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedInput, ExtendedOutput}
import org.ergoplatform.explorer.db.models.{Asset, Transaction}
import org.ergoplatform.explorer.http.api.v0.models.TransactionSummary.MiniBlockInfo
import org.ergoplatform.explorer.protocol.constants
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class TransactionSummary(
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

object TransactionSummary {

  final case class MiniBlockInfo(id: Id, height: Int)

  final private case class TxStats(totalCoins: Long, totalFee: Long, feePerByte: Double)

  implicit val decoder: Decoder[TransactionSummary] = deriveDecoder

  implicit val encoder: Encoder[TransactionSummary] = { ts =>
    Json.obj(
      "summary" -> Json.obj(
        "id"                 -> ts.id.asJson,
        "timestamp"          -> ts.timestamp.asJson,
        "size"               -> ts.size.asJson,
        "confirmationsCount" -> ts.confirmationsCount.asJson,
        "block"              -> ts.miniBlockInfo.asJson
      ),
      "ioSummary" -> Json.obj(
        "totalCoinsTransferred" -> ts.totalCoins.asJson,
        "totalFee"              -> ts.totalFee.asJson,
        "feePerByte"            -> ts.feePerByte.asJson
      ),
      "inputs"  -> ts.inputs.asJson,
      "outputs" -> ts.outputs.asJson
    )
  }

  implicit val codec: Codec[TransactionSummary] = Codec.from(decoder, encoder)

  implicit val codecBlockInfo: Codec[MiniBlockInfo] = deriveCodec

  implicit val schemaBlockInfo: Schema[MiniBlockInfo] =
    implicitly[Derived[Schema[MiniBlockInfo]]].value
      .modify(_.id)(_.description("Block ID"))
      .modify(_.height)(_.description("Block height"))

  implicit val schema: Schema[TransactionSummary] =
    implicitly[Derived[Schema[TransactionSummary]]].value
      .modify(_.id)(_.description("Transaction ID"))
      .modify(_.timestamp)(
        _.description("Timestamp the transaction got into the network")
      )
      .modify(_.confirmationsCount)(_.description("Number of transaction confirmations"))
      .modify(_.size)(_.description("Size of transaction in bytes"))
      .modify(_.totalCoins)(_.description("Total amount of nanoErgs in transaction"))
      .modify(_.totalFee)(_.description("Total amount of fee in transaction in nanoErgs"))
      .modify(_.feePerByte)(_.description("Amount of nanoErgs paid for each byte of transaction"))

  def apply(
    tx: Transaction,
    numConfirmations: Int,
    inputs: List[ExtendedInput],
    outputs: List[ExtendedOutput],
    assets: List[Asset]
  ): TransactionSummary = {
    val ins  = inputs.map(InputInfo.apply)
    val outs = OutputInfo.batch(outputs, assets)
    apply(tx, numConfirmations, ins, outs)
  }

  private def apply(
    tx: Transaction,
    numConfirmations: Int,
    inputs: List[InputInfo],
    outputs: List[OutputInfo]
  ): TransactionSummary = {
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
}
