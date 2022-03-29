package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class AddressInfo(
  hasUnconfirmedTxs: Boolean,
  used: Boolean,
  confirmedBalance: Balance
)

object AddressInfo {

  implicit val schema: Schema[AddressInfo] =
    Schema
      .derived[AddressInfo]
      .modify(_.hasUnconfirmedTxs)(_.description("BOOLEAN unconfirmed transactions"))
      .modify(_.used)(_.description("BOOLEAN"))
      .modify(_.confirmedBalance)(_.description("Confirmed balance in address"))

  implicit val validator: Validator[AddressInfo] = schema.validator
}
