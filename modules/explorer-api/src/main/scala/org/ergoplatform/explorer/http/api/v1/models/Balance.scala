package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class Balance(nanoErgs: Long, tokens: List[TokenAmount])

object Balance {

  def empty: Balance = Balance(0L, List.empty)

  implicit val schema: Schema[Balance] =
    Schema
      .derived[Balance]
      .modify(_.nanoErgs)(_.description("Ergo balance"))
      .modify(_.tokens)(_.description("Tokens balances"))

  implicit val validator: Validator[Balance] = schema.validator
}
