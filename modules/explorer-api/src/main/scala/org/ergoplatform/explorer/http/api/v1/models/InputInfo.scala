package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Json}
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, FullInput}
import org.ergoplatform.explorer.http.api.models.AssetInstanceInfo
import org.ergoplatform.explorer._
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class InputInfo(
  id: BoxId,
  value: Long,
  index: Int,
  spendingProof: Option[HexString],
  outputBlockId: Id,
  outputTransactionId: TxId,
  outputIndex: Int,
  ergoTree: HexString,
  address: Address,
  assets: List[AssetInstanceInfo],
  additionalRegisters: Json
)

object InputInfo {

  implicit val codec: Codec[InputInfo] = deriveCodec

  implicit val schema: Schema[InputInfo] =
    Schema
      .derive[InputInfo]
      .modify(_.id)(_.description("ID of the corresponding box"))
      .modify(_.spendingProof)(_.description("Hex-encoded serialized sigma proof"))
      .modify(_.value)(_.description("Number of nanoErgs in the corresponding box"))
      .modify(_.index)(_.description("Index of the input in a transaction"))
      .modify(_.outputTransactionId)(
        _.description("ID of the transaction outputting corresponding box")
      )
      .modify(_.outputIndex)(_.description("Index of the output corresponding this input"))
      .modify(_.address)(_.description("Decoded address of the corresponding box holder"))

  implicit val validator: Validator[InputInfo] = Validator.derive

  def apply(i: FullInput, assets: List[ExtendedAsset]): InputInfo =
    InputInfo(
      i.input.boxId,
      i.value,
      i.input.index,
      i.input.proofBytes,
      i.outputHeaderId,
      i.outputTxId,
      i.outputIndex,
      i.ergoTree,
      i.address,
      assets.sortBy(_.index).map(AssetInstanceInfo(_)),
      i.additionalRegisters
    )

  def batch(ins: List[FullInput], assets: List[ExtendedAsset]): List[InputInfo] = {
    val groupedAssets = assets.groupBy(_.boxId)
    ins.sortBy(_.input.index).map(i => InputInfo(i, groupedAssets.getOrElse(i.input.boxId, List.empty)))
  }
}
