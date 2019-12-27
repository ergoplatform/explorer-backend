package org.ergoplatform.explorer.http.api.v0.models

import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class TransactionInfo(
  id: String,
  headerId: String,
  timestamp: Long,
  confirmationsQty: Long,
  inputs: List[InputInfo],
  outputs: List[OutputInfo]
)

object TransactionInfo {

  implicit val schema: Schema[TransactionInfo] =
    implicitly[Derived[Schema[TransactionInfo]]].value
}
