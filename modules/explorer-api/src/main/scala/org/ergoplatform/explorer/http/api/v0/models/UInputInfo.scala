package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.{BoxId, TxId}
import org.ergoplatform.explorer.db.models.UInput
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class UInputInfo(boxId: BoxId, txId: TxId, spendingProof: SpendingProofInfo)

object UInputInfo {

  implicit val codec: Codec[UInputInfo] = deriveCodec

  implicit val schema: Schema[UInputInfo] =
    implicitly[Derived[Schema[UInputInfo]]].value
    .modify(_.boxId)(_.description("Id of the corresponding box"))
    .modify(_.txId)(_.description("Id of the transaction spending this input"))

  def apply(in: UInput): UInputInfo =
    UInputInfo(in.boxId, in.txId, SpendingProofInfo(in.proofBytes, in.extension))
}
