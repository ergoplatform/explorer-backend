package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.TxId
import sttp.tapir.{Schema, Validator}

final case class TxIdResponse(id: TxId)

object TxIdResponse {

  implicit val codec: Codec[TxIdResponse] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[TxIdResponse] =
    Schema
      .derived[TxIdResponse]
      .modify(_.id)(_.description("Id of submitted transaction"))

  implicit val validator: Validator[TxIdResponse] = schema.validator
}
