package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.Json
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, ExtendedUAsset, ExtendedUInput}
import org.ergoplatform.explorer.http.api.models.AssetInstanceInfo
import org.ergoplatform.explorer.protocol.models.ExpandedRegister
import sttp.tapir.{Schema, SchemaType, Validator}

@derive(encoder, decoder)
final case class UInputInfo(
  boxId: BoxId,
  value: Long,
  index: Int,
  spendingProof: Option[HexString],
  outputBlockId: Option[BlockId],
  outputTransactionId: TxId,
  outputIndex: Int,
  ergoTree: ErgoTree,
  address: Address,
  assets: List[AssetInstanceInfo],
  additionalRegisters: Json
)

object UInputInfo {

  implicit val schema: Schema[UInputInfo] =
    Schema
      .derived[UInputInfo]
      .modify(_.boxId)(_.description("ID of the corresponding box"))
      .modify(_.spendingProof)(_.description("Hex-encoded serialized sigma proof"))
      .modify(_.value)(_.description("Number of nanoErgs in the corresponding box"))
      .modify(_.index)(_.description("Index of the input in a transaction"))
      .modify(_.outputTransactionId)(
        _.description("ID of the transaction outputting corresponding box")
      )
      .modify(_.outputIndex)(_.description("Index of the output corresponding this input"))
      .modify(_.address)(_.description("Decoded address of the corresponding box holder"))

  implicit val validator: Validator[UInputInfo] = schema.validator

  implicit private def registersSchema: Schema[Json] =
    Schema(
      SchemaType.SOpenProduct(
        Schema.derived[ExpandedRegister]
          .modify(_.serializedValue)(_.description("Hex-encoded serialized register value"))
          .modify(_.sigmaType)(_.description("Type of the register value"))
          .modify(_.renderedValue)(_.description("Human-readable rendered register value"))
          .as[Json]
      )(_ => Map.empty)
    )

  def apply(i: ExtendedUInput, assets: List[ExtendedUAsset], confirmedAssets: List[ExtendedAsset]): UInputInfo =
    UInputInfo(
      i.input.boxId,
      i.value,
      i.input.index,
      i.input.proofBytes,
      i.outputBlockId,
      i.outputTxId,
      i.outputIndex,
      i.ergoTree,
      i.address,
      assets.sortBy(_.index).map(AssetInstanceInfo(_)) ++ confirmedAssets.sortBy(_.index).map(AssetInstanceInfo(_)),
      i.additionalRegisters
    )

  def batch(
    ins: List[ExtendedUInput],
    assets: List[ExtendedUAsset],
    confirmedAssets: List[ExtendedAsset]
  ): List[UInputInfo] = {
    val groupedAssets     = assets.groupBy(_.boxId)
    val groupedConfAssets = confirmedAssets.groupBy(_.boxId)
    ins
      .sortBy(_.input.index)
      .map { i =>
        UInputInfo(
          i,
          groupedAssets.getOrElse(i.input.boxId, List.empty),
          groupedConfAssets.getOrElse(i.input.boxId, List.empty)
        )
      }
  }
}
