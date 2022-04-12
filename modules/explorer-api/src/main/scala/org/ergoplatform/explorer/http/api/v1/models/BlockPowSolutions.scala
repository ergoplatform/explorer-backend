package org.ergoplatform.explorer.http.api.v1.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.HexString
import sttp.tapir.{Schema, Validator}

case class BlockPowSolutions(
  pk: HexString,
  w: HexString,
  n: HexString,
  d: String
)

object BlockPowSolutions {

  implicit val codec: Codec[BlockPowSolutions] = deriveCodec

  implicit val schema: Schema[BlockPowSolutions] = Schema.derived[BlockPowSolutions]

  implicit val validator: Validator[BlockPowSolutions] = schema.validator
}
