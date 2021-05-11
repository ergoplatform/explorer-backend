package org.ergoplatform.explorer.http.api.v1.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.Address
import sttp.tapir.{Schema, Validator}

final case class MinerInfo(address: Address, name: String)

object MinerInfo {

  implicit val codec: Codec[MinerInfo] = deriveCodec

  implicit val schema: Schema[MinerInfo] =
    Schema.derived[MinerInfo]
      .modify(_.address)(_.description("Miner reward address"))
      .modify(_.name)(_.description("Miner name"))

  implicit val validator: Validator[MinerInfo] = schema.validator
}

