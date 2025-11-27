package org.ergoplatform.explorer.http.api.v1.models

import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Json}
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, FullDataInput}
import org.ergoplatform.explorer.http.api.models.AssetInstanceInfo
import org.ergoplatform.explorer.protocol.models.ExpandedRegister
import sttp.tapir.{Schema, SchemaType, Validator}

final case class DataInputInfo(
  boxId: BoxId,
  value: Long,
  index: Int,
  outputBlockId: BlockId,
  outputTransactionId: TxId,
  outputIndex: Int,
  ergoTree: HexString,
  address: Address,
  assets: List[AssetInstanceInfo],
  additionalRegisters: Json
)

object DataInputInfo {

  implicit val codec: Codec[DataInputInfo] = deriveCodec

  implicit val schema: Schema[DataInputInfo] =
    Schema
      .derived[DataInputInfo]
      .modify(_.boxId)(_.description("ID of the corresponding box"))
      .modify(_.value)(_.description("Number of nanoErgs in the corresponding box"))
      .modify(_.index)(_.description("Index of the input in a transaction"))
      .modify(_.outputTransactionId)(
        _.description("ID of the transaction outputting corresponding box")
      )
      .modify(_.outputIndex)(_.description("Index of the output corresponding this input"))
      .modify(_.address)(_.description("Decoded address of the corresponding box holder"))

  implicit val validator: Validator[DataInputInfo] = schema.validator

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

  def apply(i: FullDataInput, assets: List[ExtendedAsset]): DataInputInfo =
    DataInputInfo(
      i.input.boxId,
      i.value,
      i.input.index,
      i.outputHeaderId,
      i.outputTxId,
      i.outputIndex,
      i.ergoTree,
      i.address,
      assets.sortBy(_.index).map(AssetInstanceInfo(_)),
      i.additionalRegisters
    )

  def batch(ins: List[FullDataInput], assets: List[ExtendedAsset]): List[DataInputInfo] = {
    val groupedAssets = assets.groupBy(_.boxId)
    ins.sortBy(_.input.index).map(i => DataInputInfo(i, groupedAssets.getOrElse(i.input.boxId, List.empty)))
  }
}
