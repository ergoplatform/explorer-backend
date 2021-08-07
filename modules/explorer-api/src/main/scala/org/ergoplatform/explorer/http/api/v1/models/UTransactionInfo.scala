package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.explorer
import org.ergoplatform.explorer.db.models.{Transaction, UTransaction}
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, ExtendedOutput, FullDataInput, FullInput}
import org.ergoplatform.explorer.{BlockId, TxId}
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class UTransactionInfo(
  id: TxId,
  creationTimestamp: Long,
  inputs: List[UInputInfo],
  dataInputs: List[DataInputInfo],
  outputs: List[OutputInfo],
  size: Int
)

object UTransactionInfo {

  implicit val schema: Schema[UTransactionInfo] =
    Schema
      .derived[UTransactionInfo]
      .modify(_.id)(_.description("Transaction ID"))
      .modify(_.creationTimestamp)(_.description("Timestamp the transaction got into the network"))
      .modify(_.size)(_.description("Transaction size in bytes"))

  implicit val validator: Validator[UTransactionInfo] = schema.validator
}
