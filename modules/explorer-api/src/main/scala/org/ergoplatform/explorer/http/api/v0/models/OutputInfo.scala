package org.ergoplatform.explorer.http.api.v0.models

import io.circe.{Codec, Json}
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.{Address, BoxId, HexString, TxId}
import org.ergoplatform.explorer.db.models.Asset
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import sttp.tapir.{Schema, SchemaType}
import sttp.tapir.generic.Derived

final case class OutputInfo(
  id: BoxId,
  txId: TxId,
  value: Long,
  index: Int,
  creationHeight: Int,
  ergoTree: HexString,
  address: Option[Address],
  assets: List[AssetInfo],
  additionalRegisters: Json,
  spentTransactionId: Option[TxId],
  mainChain: Boolean
)

object OutputInfo {

  implicit val codec: Codec[OutputInfo] = deriveCodec

  implicit val schema: Schema[OutputInfo] =
    implicitly[Derived[Schema[OutputInfo]]].value
      .modify(_.id)(_.description("Id of the box"))
      .modify(_.txId)(_.description("Id of the transaction that created the box"))
      .modify(_.value)(_.description("Value of the box in nanoERG"))
      .modify(_.index)(_.description("Index of the output in a transaction"))
      .modify(_.creationHeight)(_.description("Height at which the box was created"))
      .modify(_.ergoTree)(_.description("Serialized ergo tree"))
      .modify(_.address)(_.description("An address derived from ergo tree"))

  implicit private def registersSchema: Schema[Json] =
    Schema(
      SchemaType.SOpenProduct(
        SchemaType.SObjectInfo("AdditionalRegisters"),
        Schema(SchemaType.SString)
      )
    )

  def apply(
    o: ExtendedOutput,
    assets: List[Asset]
  ): OutputInfo =
    OutputInfo(
      o.output.boxId,
      o.output.txId,
      o.output.value,
      o.output.index,
      o.output.creationHeight,
      o.output.ergoTree,
      o.output.addressOpt,
      assets.sortBy(_.index).map(x => AssetInfo(x.tokenId, x.index, x.amount)),
      o.output.additionalRegisters,
      o.spentByOpt,
      o.output.mainChain
    )

  def batch(outputs: List[ExtendedOutput], assets: List[Asset]): List[OutputInfo] = {
    val groupedAssets = assets.groupBy(_.boxId)
    outputs
      .sortBy(_.output.index)
      .map(out => OutputInfo(out, groupedAssets.get(out.output.boxId).toList.flatten))
  }
}
