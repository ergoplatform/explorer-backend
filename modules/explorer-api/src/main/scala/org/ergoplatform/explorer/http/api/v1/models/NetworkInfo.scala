package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.db.models.EpochParameters
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class NetworkInfo(
  lastBlockId: Id,
  height: Int,
  maxBoxGix: Long,
  maxTxGix: Long,
  params: EpochParameters
)

object NetworkInfo {

  implicit def schema: Schema[NetworkInfo]       = Schema.derived[NetworkInfo]
  implicit def validator: Validator[NetworkInfo] = schema.validator
}
