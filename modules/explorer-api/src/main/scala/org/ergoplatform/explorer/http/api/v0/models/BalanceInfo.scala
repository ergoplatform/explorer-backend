package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.Address
import sttp.tapir.{Schema, Validator}

final case class BalanceInfo(address: Address, balance: Long)

object BalanceInfo {

  implicit val codec: Codec[BalanceInfo] = deriveCodec

  implicit val schema: Schema[BalanceInfo] =
    Schema
      .derive[BalanceInfo]
      .modify(_.address)(_.description("Address"))
      .modify(_.balance)(_.description("Balance in nanoERG"))

  implicit val validator: Validator[BalanceInfo] =
    Validator.derive
}
