package org.ergoplatform.explorer.http.api.v0.models

import io.circe.{Codec, Json}
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.db.models.aggregates.ExtendedUInput
import org.ergoplatform.explorer.{Address, BoxId, TxId}
import sttp.tapir.{Schema, Validator}

final case class UInputInfo(
  id: BoxId,
  transactionId: TxId,
  spendingProof: SpendingProofInfo,
  value: Option[Long],
  index: Int,
  outputTransactionId: Option[TxId],
  outputIndex: Option[Int],
  address: Option[Address],
  extension: Json
)

object UInputInfo {

  implicit val codec: Codec[UInputInfo] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[UInputInfo] =
    Schema
      .derived[UInputInfo]
      .modify(_.id)(_.description("Id of the corresponding box"))
      .modify(_.transactionId)(_.description("Id of the transaction spending this input"))
      .modify(_.value)(_.description("Amount of nanoERGs in the corresponding box"))
      .modify(_.index)(_.description("Index of the input in a transaction"))
      .modify(_.outputTransactionId)(_.description("ID of the output transaction"))
      .modify(_.outputIndex)(_.description("Index of the output corresponding this input"))
      .modify(_.address)(_.description("Address"))
      .modify(_.extension)(_.description("Context extension of the spending proof"))

  implicit val validator: Validator[UInputInfo] = schema.validator

  def apply(in: ExtendedUInput): UInputInfo =
    new UInputInfo(
      in.input.boxId,
      in.input.txId,
      SpendingProofInfo(in.input.proofBytes, in.input.extension),
      Some(in.value),
      in.input.index,
      Some(in.outputTxId),
      Some(in.outputIndex),
      Some(in.address),
      in.extension
    )

  def batch(ins: List[ExtendedUInput]): List[UInputInfo] =
    ins.sortBy(_.input.index).map(apply)
}
