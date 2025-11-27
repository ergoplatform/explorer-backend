package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.Json
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, FullInput}
import org.ergoplatform.explorer.http.api.models.AssetInstanceInfo
import org.ergoplatform.explorer.protocol.models.ExpandedRegister
import sttp.tapir.{Schema, SchemaType, Validator}

@derive(encoder, decoder)
final case class InputInfo(
  boxId: BoxId,
  value: Long,
  index: Int,
  spendingProof: Option[HexString],
  outputBlockId: BlockId,
  outputTransactionId: TxId,
  outputIndex: Int,
  outputGlobalIndex: Long,
  outputCreatedAt: Int,
  outputSettledAt: Int,
  ergoTree: HexString,
  ergoTreeConstants: String,
  ergoTreeScript: String,
  address: Address,
  assets: List[AssetInstanceInfo],
  additionalRegisters: Json
)

object InputInfo {

  implicit val schema: Schema[InputInfo] =
    Schema
      .derived[InputInfo]
      .modify(_.boxId)(_.description("ID of the corresponding box"))
      .modify(_.spendingProof)(_.description("Hex-encoded serialized sigma proof"))
      .modify(_.value)(_.description("Number of nanoErgs in the corresponding box"))
      .modify(_.index)(_.description("Index of the input in a transaction"))
      .modify(_.outputTransactionId)(
        _.description("ID of the transaction outputting corresponding box")
      )
      .modify(_.outputIndex)(_.description("Index of the output corresponding this input"))
      .modify(_.outputGlobalIndex)(_.description("Global index of the output corresponding this input"))
      .modify(_.outputCreatedAt)(_.description("Height the output corresponding this input was created at"))
      .modify(_.outputSettledAt)(_.description("Height the output corresponding this input was settled at"))
      .modify(_.address)(_.description("Decoded address of the corresponding box holder"))

  implicit val validator: Validator[InputInfo] = schema.validator

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

  def apply(i: FullInput, assets: List[ExtendedAsset]): InputInfo = {
    val (ergoTreeConstants, ergoTreeScript) = PrettyErgoTree.fromHexString(i.ergoTree).fold(
      _ => ("", ""),
      tree => (tree.constants, tree.script)
    )
    InputInfo(
      i.input.boxId,
      i.value,
      i.input.index,
      i.input.proofBytes,
      i.outputHeaderId,
      i.outputTxId,
      i.outputIndex,
      i.outputGlobalIndex,
      i.outputCreatedAt,
      i.outputSettledAt,
      i.ergoTree,
      ergoTreeConstants,
      ergoTreeScript,
      i.address,
      assets.sortBy(_.index).map(AssetInstanceInfo(_)),
      i.additionalRegisters
    )
  }

  def batch(ins: List[FullInput], assets: List[ExtendedAsset]): List[InputInfo] = {
    val groupedAssets = assets.groupBy(_.boxId)
    ins.sortBy(_.input.index).map(i => InputInfo(i, groupedAssets.getOrElse(i.input.boxId, List.empty)))
  }
}
