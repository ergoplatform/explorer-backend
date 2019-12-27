package org.ergoplatform.explorer.http.api.v0.models

import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class InputInfo(
  id: String,
  proof: String,
  value: Option[Long],
  txId: String,
  outputTransactionId: Option[String],
  address: Option[String]
)

object InputInfo {

  implicit val schema: Schema[InputInfo] =
    implicitly[Derived[Schema[InputInfo]]].value
}
