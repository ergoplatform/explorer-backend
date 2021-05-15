package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class TotalBalance(confirmed: Balance, unconfirmed: Balance)

object TotalBalance {

  implicit val schema: Schema[TotalBalance] =
    Schema
      .derived[TotalBalance]
      .modify(_.confirmed)(_.description("Confirmed balance"))
      .modify(_.unconfirmed)(_.description("Unconfirmed balance"))

  implicit val validator: Validator[TotalBalance] = schema.validator
}
