package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.{Id, TxId}
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class TransactionInfo(
  id: TxId,
  headerId: Id,
  timestamp: Long,
  confirmationsQty: Int,
  inputs: List[InputInfo],
  outputs: List[OutputInfo]
)

object TransactionInfo {

  implicit val codec: Codec[TransactionInfo] = deriveCodec

  implicit val schema: Schema[TransactionInfo] =
    implicitly[Derived[Schema[TransactionInfo]]].value
      .modify(_.id)(_.description("Transaction ID"))
      .modify(_.headerId)(_.description("ID of the block the transaction was included in"))
      .modify(_.timestamp)(_.description("Approx timestamp the transaction got into the network"))
      .modify(_.confirmationsQty)(_.description("Number of transaction confirmations"))
}
