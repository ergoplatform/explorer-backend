package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.HexString
import sttp.tapir.{Schema, Validator}

final case class PowSolutionInfo(pk: HexString, w: HexString, n: HexString, d: String)

object PowSolutionInfo {

  implicit val codec: Codec[PowSolutionInfo] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[PowSolutionInfo] =
    Schema.derived[PowSolutionInfo]
      .modify(_.pk)(_.description("Miner public key"))
      .modify(_.d)(_.description("Autolykos.d"))

  implicit val validator: Validator[PowSolutionInfo] = schema.validator
}
