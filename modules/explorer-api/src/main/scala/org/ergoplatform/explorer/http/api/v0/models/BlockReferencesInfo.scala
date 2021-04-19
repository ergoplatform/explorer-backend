package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.Id
import sttp.tapir.{Schema, Validator}

final case class BlockReferencesInfo(previousId: Id, nextId: Option[Id])

object BlockReferencesInfo {

  implicit val codec: Codec[BlockReferencesInfo] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[BlockReferencesInfo] =
    Schema
      .derived[BlockReferencesInfo]
      .modify(_.previousId)(_.description("ID of the previous block"))
      .modify(_.nextId)(_.description("ID of the next block (if one exists)"))

  implicit val validator: Validator[BlockReferencesInfo] = schema.validator
}
