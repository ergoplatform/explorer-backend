package org.ergoplatform.explorer.http.api.v1.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.db.models.aggregates.ExtendedDataInput
import org.ergoplatform.explorer.{Address, BoxId, TxId}
import sttp.tapir.{Schema, Validator}

final case class DataInputInfo(
  id: BoxId,
  value: Option[Long],
  index: Int,
  transactionId: TxId,
  outputTransactionId: Option[TxId],
  outputIndex: Option[Int],
  address: Option[Address]
)

object DataInputInfo {

  implicit val codec: Codec[DataInputInfo] = deriveCodec

  implicit val schema: Schema[DataInputInfo] =
    Schema
      .derive[DataInputInfo]
      .modify(_.id)(_.description("ID of the corresponding box"))
      .modify(_.value)(_.description("Number of nanoErgs in the corresponding box"))
      .modify(_.transactionId)(_.description("ID of the transaction this data input was used in"))
      .modify(_.outputTransactionId)(
        _.description("ID of the transaction outputting corresponding box")
      )
      .modify(_.address)(_.description("Decoded address of the corresponding box holder"))

  implicit val validator: Validator[DataInputInfo] = Validator.derive

  def apply(i: ExtendedDataInput): DataInputInfo =
    DataInputInfo(
      i.input.boxId,
      i.value,
      i.input.index,
      i.input.txId,
      i.outputTxId,
      i.outputIndex,
      i.address
    )

  def batch(ins: List[ExtendedDataInput]): List[DataInputInfo] =
    ins.sortBy(_.input.index).map(apply)
}
