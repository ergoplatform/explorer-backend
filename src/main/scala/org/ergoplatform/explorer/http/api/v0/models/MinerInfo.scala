package org.ergoplatform.explorer.http.api.v0.models

import org.ergoplatform.explorer.Address
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class MinerInfo(addressId: Address, name: String)

object MinerInfo {

  implicit val schema: Schema[MinerInfo] =
    implicitly[Derived[Schema[MinerInfo]]].value
      .modify(_.addressId)(_.description("Miner reward address"))
      .modify(_.name)(_.description("Miner name"))
}
