package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.Json
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.db.models.{Output, UOutput}
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, ExtendedOutput, ExtendedUAsset, ExtendedUOutput}
import org.ergoplatform.explorer.http.api.models.AssetInstanceInfo
import org.ergoplatform.explorer.protocol.models.ExpandedRegister
import sttp.tapir.{Schema, SchemaType, Validator}

@derive(encoder, decoder)
final case class UOutputInfo(
  boxId: BoxId,
  transactionId: TxId,
  value: Long,
  index: Int,
  creationHeight: Int,
  ergoTree: HexString,
  address: Address,
  assets: List[AssetInstanceInfo],
  additionalRegisters: Json,
  spentTransactionId: Option[TxId]
)

object UOutputInfo {

  implicit val schema: Schema[UOutputInfo] =
    Schema
      .derived[UOutputInfo]
      .modify(_.boxId)(_.description("Id of the box"))
      .modify(_.transactionId)(_.description("Id of the transaction that created the box"))
      .modify(_.value)(_.description("Value of the box in nanoERG"))
      .modify(_.index)(_.description("Index of the output in a transaction"))
      .modify(_.creationHeight)(_.description("Height at which the box was created"))
      .modify(_.ergoTree)(_.description("Serialized ergo tree"))
      .modify(_.address)(_.description("An address derived from ergo tree"))
      .modify(_.spentTransactionId)(_.description("Id of the transaction this output was spent by"))

  implicit val validator: Validator[UOutputInfo] = schema.validator

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

  def apply(
    o: ExtendedUOutput,
    assets: List[ExtendedUAsset]
  ): UOutputInfo =
    unspent(o.output, assets).copy(spentTransactionId = o.spendingTxId)

  def unspent(
    o: UOutput,
    assets: List[ExtendedUAsset]
  ): UOutputInfo =
    UOutputInfo(
      o.boxId,
      o.txId,
      o.value,
      o.index,
      o.creationHeight,
      o.ergoTree,
      o.address,
      assets.sortBy(_.index).map(AssetInstanceInfo(_)),
      o.additionalRegisters,
      None
    )
}
