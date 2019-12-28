package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class AdProofInfo(
  headerId: String,
  proofBytes: String,
  digest: String
)

object AdProofInfo {

  implicit val codec: Codec[AdProofInfo] = deriveCodec

  implicit val schema: Schema[AdProofInfo] =
    implicitly[Derived[Schema[AdProofInfo]]].value
}
