package org.ergoplatform.explorer.http.api.v0.models

import io.circe.{Codec, Json}
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.{Address, BoxId, ContractAttributes, HexString, TxId}
import org.ergoplatform.explorer.db.models.Asset
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import sttp.tapir.{Schema, SchemaType}
import sttp.tapir.generic.Derived

final case class OutputInfo(
  id: BoxId,
  value: Long,
  creationHeight: Int,
  ergoTree: HexString,
  contractAttributes: Option[ContractAttributes],
  address: Option[Address],
  assets: Seq[AssetInfo],
  additionalRegisters: Json,
  spentTxId: Option[TxId],
  mainChain: Boolean
)

object OutputInfo {

  implicit val codec: Codec[OutputInfo] = deriveCodec

  implicit val schema: Schema[OutputInfo] =
    implicitly[Derived[Schema[OutputInfo]]].value

  implicit private def registersSchema: Schema[Json] =
    Schema(
      SchemaType.SOpenProduct(
        SchemaType.SObjectInfo("AdditionalRegisters"),
        Schema(SchemaType.SString)
      )
    )

  def apply(
    o: ExtendedOutput,
    assets: List[Asset],
    contractAttributes: Option[ContractAttributes] = None
  ): OutputInfo =
    OutputInfo(
      o.output.boxId,
      o.output.value,
      o.output.creationHeight,
      o.output.ergoTree,
      contractAttributes,
      o.output.addressOpt,
      assets.map(x => AssetInfo(x.tokenId, x.amount)),
      o.output.additionalRegisters,
      o.spentByOpt,
      o.output.mainChain
    )

  def batch(outputs: List[ExtendedOutput], assets: List[Asset]): List[OutputInfo] = {
    val groupedAssets = assets.groupBy(_.boxId)
    outputs
      .map(out => OutputInfo(out, groupedAssets.get(out.output.boxId).toList.flatten))
  }
}
