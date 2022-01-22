package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.explorer.BlockId
import org.ergoplatform.explorer.db.models.EpochParameters
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class NetworkState(
  lastBlockId: BlockId,
  height: Int,
  maxBoxGix: Long,
  maxTxGix: Long,
  params: EpochInfo
)

object NetworkState {

  implicit def schema: Schema[NetworkState]       = Schema.derived[NetworkState]
  implicit def validator: Validator[NetworkState] = schema.validator
}
