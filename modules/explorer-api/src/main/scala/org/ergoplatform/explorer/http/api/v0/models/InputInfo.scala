package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.db.models.aggregates.ExtendedInput
import org.ergoplatform.explorer.{Address, BoxId, HexString, TxId}
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

// TODO ScalaDoc
final case class InputInfo(
  id: BoxId,
  proof: Option[HexString],
  value: Option[Long],   // TODO ScalaDoc: what it means the value is None
  txId: TxId,
  outputTransactionId: Option[TxId], // TODO ScalaDoc: describe the meaning of None
  address: Option[Address]  // TODO ScalaDoc: describe the meaning of None
)

object InputInfo {

  implicit val codec: Codec[InputInfo] = deriveCodec

  implicit val schema: Schema[InputInfo] =
    implicitly[Derived[Schema[InputInfo]]].value
      .modify(_.id)(_.description("ID of the corresponding box"))
      .modify(_.proof)(_.description("Hex-encoded serialized sigma proof"))
      .modify(_.value)(_.description("Number of nanoErgs in the corresponding box"))
      .modify(_.txId)(_.description("ID of the transaction this input was used in"))
      .modify(_.outputTransactionId)(
        _.description("ID of the transaction outputting corresponding box")
      )
      .modify(_.address)(_.description("Decoded address of the corresponding box holder"))

  def apply(i: ExtendedInput): InputInfo =
    InputInfo(
      i.input.boxId,
      i.input.proofBytes,
      i.value,
      i.input.txId,
      i.outputTxId,
      i.address
    )
}
