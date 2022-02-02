package org.ergoplatform.explorer.http.api.v0.models

import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import io.circe.{Codec, Json}
import cats.syntax.option._
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedUAsset, ExtendedUOutput}
import org.ergoplatform.explorer.http.api.models.AssetInstanceInfo
import org.ergoplatform.explorer.protocol.registers
import org.ergoplatform.explorer.{Address, BoxId, HexString, TxId}
import sttp.tapir.{Schema, SchemaType, Validator}

final case class UOutputInfo(
  id: BoxId,
  txId: TxId,
  value: Long,
  index: Int,
  creationHeight: Int,
  ergoTree: HexString,
  address: Option[Address],
  assets: List[AssetInstanceInfo],
  additionalRegisters: Json
)

object UOutputInfo {

  implicit val codec: Codec[UOutputInfo] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[UOutputInfo] =
    Schema
      .derived[UOutputInfo]
      .modify(_.id)(_.description("Id of the corresponding box"))
      .modify(_.txId)(_.description("Id of the transaction that created the box"))
      .modify(_.value)(_.description("Amount of nanoERGs containing in the box"))
      .modify(_.creationHeight)(_.description("Approximate height the box was created"))
      .modify(_.ergoTree)(_.description("Encoded script"))
      .modify(_.address)(_.description("Address derived from ErgoTree"))
      .modify(_.additionalRegisters)(_.description("Arbitrary key->value dictionary"))

  implicit val validator: Validator[UOutputInfo] = schema.validator

  implicit private def registersSchema: Schema[Json] =
    Schema(
      SchemaType.SOpenProduct(
        Schema(SchemaType.SString[Json]())
      )(_ => Map.empty)
    )

  def apply(out: ExtendedUOutput, assets: List[ExtendedUAsset]): UOutputInfo =
    UOutputInfo(
      out.output.boxId,
      out.output.txId,
      out.output.value,
      out.output.index,
      out.output.creationHeight,
      out.output.ergoTree,
      out.output.address.some,
      assets.sortBy(_.index).map(AssetInstanceInfo(_)),
      registers.convolveJson(out.output.additionalRegisters)
    )

  def batch(outputs: List[ExtendedUOutput], assets: List[ExtendedUAsset]): List[UOutputInfo] = {
    val groupedAssets = assets.groupBy(_.boxId)
    outputs.map(out => apply(out, groupedAssets.get(out.output.boxId).toList.flatten))
  }
}
