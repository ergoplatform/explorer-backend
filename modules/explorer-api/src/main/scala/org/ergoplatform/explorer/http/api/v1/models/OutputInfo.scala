package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.Json
import org.ergoplatform.explorer.db.models.Output
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, ExtendedOutput}
import org.ergoplatform.explorer.http.api.models.AssetInstanceInfo
import org.ergoplatform.explorer.{Address, BoxId, HexString, Id, TxId}
import sttp.tapir.json.circe.validatorForCirceJson
import sttp.tapir.{Schema, SchemaType, Validator}

@derive(encoder, decoder)
final case class OutputInfo(
  boxId: BoxId,
  transactionId: TxId,
  blockId: Id,
  value: Long,
  index: Int,
  creationHeight: Int,
  ergoTree: HexString,
  address: Address,
  assets: List[AssetInstanceInfo],
  additionalRegisters: Json,
  spentTransactionId: Option[TxId],
  mainChain: Boolean
)

object OutputInfo {

  implicit val schema: Schema[OutputInfo] =
    Schema
      .derive[OutputInfo]
      .modify(_.boxId)(_.description("Id of the box"))
      .modify(_.transactionId)(_.description("Id of the transaction that created the box"))
      .modify(_.blockId)(_.description("Id of the block a box included in"))
      .modify(_.value)(_.description("Value of the box in nanoERG"))
      .modify(_.index)(_.description("Index of the output in a transaction"))
      .modify(_.creationHeight)(_.description("Height at which the box was created"))
      .modify(_.ergoTree)(_.description("Serialized ergo tree"))
      .modify(_.address)(_.description("An address derived from ergo tree"))
      .modify(_.spentTransactionId)(_.description("Id of the transaction this output was spent by"))

  implicit val validator: Validator[OutputInfo] = Validator.derive

  implicit private def registersSchema: Schema[Json] =
    Schema(
      SchemaType.SOpenProduct(
        SchemaType.SObjectInfo("AdditionalRegisters"),
        Schema(SchemaType.SString)
      )
    )

  def apply(
    o: ExtendedOutput,
    assets: List[ExtendedAsset]
  ): OutputInfo =
    OutputInfo(
      o.output.boxId,
      o.output.txId,
      o.output.headerId,
      o.output.value,
      o.output.index,
      o.output.creationHeight,
      o.output.ergoTree,
      o.output.address,
      assets.sortBy(_.index).map(AssetInstanceInfo(_)),
      o.output.additionalRegisters,
      o.spentByOpt,
      o.output.mainChain
    )

  def unspent(
    o: Output,
    assets: List[ExtendedAsset]
  ): OutputInfo =
    OutputInfo(
      o.boxId,
      o.txId,
      o.headerId,
      o.value,
      o.index,
      o.creationHeight,
      o.ergoTree,
      o.address,
      assets.sortBy(_.index).map(AssetInstanceInfo(_)),
      o.additionalRegisters,
      None,
      o.mainChain
    )

  def batch(outputs: List[ExtendedOutput], assets: List[ExtendedAsset]): List[OutputInfo] = {
    val groupedAssets = assets.groupBy(_.boxId)
    outputs
      .sortBy(_.output.index)
      .map(out => OutputInfo(out, groupedAssets.get(out.output.boxId).toList.flatten))
  }
}
