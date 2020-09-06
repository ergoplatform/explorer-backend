package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.Address
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class BalanceInfo(address: Address, balance: Long)

object BalanceInfo {

  implicit val codec: Codec[BalanceInfo] = deriveCodec

  implicit val schema: Schema[BalanceInfo] =
    implicitly[Derived[Schema[BalanceInfo]]].value
      .modify(_.address)(_.description("Address"))
      .modify(_.balance)(_.description("Balance in nanoERG"))
}