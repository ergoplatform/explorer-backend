package org.ergoplatform.explorer.http.api.v0.models

import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import io.circe.{Codec, Json}
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, ExtendedOutput}
import org.ergoplatform.explorer.http.api.models.AssetInstanceInfo
import org.ergoplatform.explorer.protocol.registers
import org.ergoplatform.explorer.{Address, BoxId, HexString, TxId}
import sttp.tapir.{Schema, SchemaType, Validator}

final case class OutputInfo(
  id: BoxId,
  txId: TxId,
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

  implicit val codec: Codec[OutputInfo] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[OutputInfo] =
    Schema
      .derived[OutputInfo]
      .modify(_.id)(_.description("Id of the box"))
      .modify(_.txId)(_.description("Id of the transaction that created the box"))
      .modify(_.value)(_.description("Value of the box in nanoERG"))
      .modify(_.index)(_.description("Index of the output in a transaction"))
      .modify(_.creationHeight)(_.description("Height at which the box was created"))
      .modify(_.ergoTree)(_.description("Serialized ergo tree"))
      .modify(_.address)(_.description("An address derived from ergo tree"))

  implicit val validator: Validator[OutputInfo] = schema.validator

  implicit private def registersSchema: Schema[Json] =
    Schema(
      SchemaType.SOpenProduct(
        Schema(SchemaType.SString[Json]())
      )(_ => Map.empty)
    )

  def apply(
    o: ExtendedOutput,
    assets: List[ExtendedAsset]
  ): OutputInfo =
    OutputInfo(
      o.output.boxId,
      o.output.txId,
      o.output.value,
      o.output.index,
      o.output.creationHeight,
      o.output.ergoTree,
      o.output.address,
      assets.sortBy(_.index).map(AssetInstanceInfo(_)),
      registers.convolveJson(o.output.additionalRegisters),
      o.spentByOpt,
      o.output.mainChain
    )

  def batch(outputs: List[ExtendedOutput], assets: List[ExtendedAsset]): List[OutputInfo] = {
    val groupedAssets = assets.groupBy(_.boxId)
    outputs
      .sortBy(_.output.index)
      .map(out => OutputInfo(out, groupedAssets.get(out.output.boxId).toList.flatten))
  }
}
