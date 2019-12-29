package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.HexString
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class PowSolutionInfo(pk: HexString, w: String, n: String, d: String)

object PowSolutionInfo {

  implicit val codec: Codec[PowSolutionInfo] = deriveCodec

  implicit val schema: Schema[PowSolutionInfo] =
    implicitly[Derived[Schema[PowSolutionInfo]]].value
      .modify(_.pk)(_.description("Miner public key"))
}
