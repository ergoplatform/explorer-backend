package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.Address
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

// TODO ScalaDoc
final case class MinerInfo(addressId: Address, name: String)

object MinerInfo {

  implicit val codec: Codec[MinerInfo] = deriveCodec

  implicit val schema: Schema[MinerInfo] =
    implicitly[Derived[Schema[MinerInfo]]].value
      .modify(_.addressId)(_.description("Miner reward address"))
      .modify(_.name)(_.description("Miner name"))
}
