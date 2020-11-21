package org.ergoplatform.explorer.http.api.v1.models

import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Json}
import org.ergoplatform.explorer.db.models.{Asset, Output}
import org.ergoplatform.explorer.http.api.v0.models.AssetInfo
import org.ergoplatform.explorer.{Address, BoxId, HexString, TxId}
import sttp.tapir.{Schema, SchemaType, Validator}
import sttp.tapir.json.circe.validatorForCirceJson

final case class UnspentOutputInfo(
  id: BoxId,
  txId: TxId,
  value: Long,
  index: Int,
  creationHeight: Int,
  ergoTree: HexString,
  address: Option[Address],
  assets: List[AssetInfo],
  additionalRegisters: Json,
  mainChain: Boolean
)

object UnspentOutputInfo {

  implicit val codec: Codec[UnspentOutputInfo] = deriveCodec

  implicit val schema: Schema[UnspentOutputInfo] =
    Schema
      .derive[UnspentOutputInfo]
      .modify(_.id)(_.description("Id of the box"))
      .modify(_.txId)(_.description("Id of the transaction that created the box"))
      .modify(_.value)(_.description("Value of the box in nanoERG"))
      .modify(_.index)(_.description("Index of the output in a transaction"))
      .modify(_.creationHeight)(_.description("Height at which the box was created"))
      .modify(_.ergoTree)(_.description("Serialized ergo tree"))
      .modify(_.address)(_.description("An address derived from ergo tree"))

  implicit val validator: Validator[UnspentOutputInfo] = Validator.derive

  implicit private def registersSchema: Schema[Json] =
    Schema(
      SchemaType.SOpenProduct(
        SchemaType.SObjectInfo("AdditionalRegisters"),
        Schema(SchemaType.SString)
      )
    )

  def apply(
    o: Output,
    assets: List[Asset]
  ): UnspentOutputInfo =
    UnspentOutputInfo(
      o.boxId,
      o.txId,
      o.value,
      o.index,
      o.creationHeight,
      o.ergoTree,
      o.addressOpt,
      assets.sortBy(_.index).map(x => AssetInfo(x.tokenId, x.index, x.amount)),
      o.additionalRegisters,
      o.mainChain
    )

  def batch(outputs: List[Output], assets: List[Asset]): List[UnspentOutputInfo] = {
    val groupedAssets = assets.groupBy(_.boxId)
    outputs
      .sortBy(_.index)
      .map(out => UnspentOutputInfo(out, groupedAssets.get(out.boxId).toList.flatten))
  }
}
