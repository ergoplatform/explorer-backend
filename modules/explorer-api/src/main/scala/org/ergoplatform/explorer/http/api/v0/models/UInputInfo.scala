package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.{Address, BoxId, TxId}
import org.ergoplatform.explorer.db.models.UInput
import org.ergoplatform.explorer.db.models.aggregates.ExtendedUInput
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class UInputInfo(
  id: BoxId,
  transactionId: TxId,
  spendingProof: SpendingProofInfo,
  value: Option[Long],
  outputTransactionId: Option[TxId],
  address: Option[Address]
)

object UInputInfo {

  implicit val codec: Codec[UInputInfo] = deriveCodec

  implicit val schema: Schema[UInputInfo] =
    implicitly[Derived[Schema[UInputInfo]]].value
      .modify(_.id)(_.description("Id of the corresponding box"))
      .modify(_.transactionId)(_.description("Id of the transaction spending this input"))
      .modify(_.value)(_.description("Amount of nanoERGs in the corresponding box"))
      .modify(_.outputTransactionId)(_.description("ID of the output transaction"))
      .modify(_.address)(_.description("Address"))

  def apply(in: ExtendedUInput): UInputInfo =
    new UInputInfo(
      in.input.boxId,
      in.input.txId,
      SpendingProofInfo(in.input.proofBytes, in.input.extension),
      in.value,
      in.outputTxId,
      in.address
    )

  def batch(ins: List[ExtendedUInput]): List[UInputInfo] =
    ins.sortBy(_.input.index).map(apply)
}
