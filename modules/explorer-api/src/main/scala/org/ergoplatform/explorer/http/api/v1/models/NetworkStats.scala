package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class NetworkStats(
  uniqueAddressesNum: Long
)

object NetworkStats {

  implicit def schema: Schema[NetworkStats]       = Schema.derived[NetworkStats]
  implicit def validator: Validator[NetworkStats] = schema.validator
}
