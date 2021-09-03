package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.db.models.aggregates.ExtendedUDataInput
import org.ergoplatform.explorer.{Address, BoxId, TxId}
import sttp.tapir.{Schema, Validator}

final case class UDataInputInfo(
  id: BoxId,
  transactionId: TxId,
  value: Option[Long],
  index: Int,
  outputTransactionId: Option[TxId],
  outputIndex: Option[Int],
  address: Option[Address]
)

object UDataInputInfo {

  implicit val codec: Codec[UDataInputInfo] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[UDataInputInfo] =
    Schema
      .derived[UDataInputInfo]
      .modify(_.id)(_.description("Id of the corresponding box"))
      .modify(_.transactionId)(_.description("ID of the transaction this data input was used in"))
      .modify(_.value)(_.description("Amount of nanoERGs in the corresponding box"))
      .modify(_.index)(_.description("Index of the input in a transaction"))
      .modify(_.outputTransactionId)(_.description("ID of the output transaction"))
      .modify(_.outputIndex)(_.description("Index of the output corresponding this input"))
      .modify(_.address)(_.description("Address"))

  implicit val validator: Validator[UDataInputInfo] = schema.validator

  def apply(in: ExtendedUDataInput): UDataInputInfo =
    new UDataInputInfo(
      in.input.boxId,
      in.input.txId,
      Some(in.value),
      in.input.index,
      Some(in.outputTxId),
      Some(in.outputIndex),
      Some(in.address)
    )

  def batch(ins: List[ExtendedUDataInput]): List[UDataInputInfo] =
    ins.sortBy(_.input.index).map(apply)
}
