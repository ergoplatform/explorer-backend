package org.ergoplatform.explorer.http.api.v1.models

import org.ergoplatform.explorer.http.api.models.AssetInstanceInfo
import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.Json
import cats.syntax.option._
import org.ergoplatform.explorer.{Address, BlockId, BoxId, HexString, TxId}
import org.ergoplatform.explorer.protocol.models.ExpandedRegister
import sttp.tapir.{Schema, SchemaType, Validator}

/** MOutputInfo is a merge of `UOutputInfo` & `OutputInfo`
  */
@derive(encoder, decoder)
final case class MOutputInfo(
  boxId: BoxId,
  transactionId: TxId,
  blockId: Option[BlockId],
  value: Long,
  index: Int,
  globalIndex: Option[Long],
  creationHeight: Int,
  settlementHeight: Option[Int],
  ergoTree: HexString,
  address: Address,
  assets: List[AssetInstanceInfo],
  additionalRegisters: Json,
  spentTransactionId: Option[TxId],
  mainChain: Boolean // false for unconfirmed
)

object MOutputInfo {

  def apply(uo: UOutputInfo): MOutputInfo = new MOutputInfo(
    uo.boxId,
    uo.transactionId,
    None,
    uo.value,
    uo.index,
    None,
    uo.creationHeight,
    None,
    uo.ergoTree,
    uo.address,
    uo.assets,
    uo.additionalRegisters,
    uo.spentTransactionId,
    mainChain = false
  )

  def apply(o: OutputInfo): MOutputInfo = new MOutputInfo(
    o.boxId,
    o.transactionId,
    o.blockId.some,
    o.value,
    o.index,
    o.globalIndex.some,
    o.creationHeight,
    o.settlementHeight.some,
    o.ergoTree,
    o.address,
    o.assets,
    o.additionalRegisters,
    o.spentTransactionId,
    o.mainChain
  )

  def fromOutputList(lo: List[OutputInfo]): List[MOutputInfo] = lo.map(MOutputInfo(_))

  def fromUOutputList(lou: List[UOutputInfo]): List[MOutputInfo] = lou.map(MOutputInfo(_))

  implicit val schema: Schema[MOutputInfo] =
    Schema
      .derived[MOutputInfo]
      .modify(_.boxId)(_.description("Id of the box"))
      .modify(_.transactionId)(_.description("Id of the transaction that created the box"))
      .modify(_.blockId)(_.description("Id of the block a box included in"))
      .modify(_.value)(_.description("Value of the box in nanoERG"))
      .modify(_.index)(_.description("Index of the output in a transaction"))
      .modify(_.globalIndex)(_.description("Global index of the output in the blockchain"))
      .modify(_.creationHeight)(_.description("Height at which the box was created"))
      .modify(_.settlementHeight)(_.description("Height at which the box got fixed in blockchain"))
      .modify(_.ergoTree)(_.description("Serialized ergo tree"))
      .modify(_.address)(_.description("An address derived from ergo tree"))
      .modify(_.spentTransactionId)(_.description("Id of the transaction this output was spent by"))

  implicit val validator: Validator[MOutputInfo] = schema.validator

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
}
