package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.explorer.db.models.EpochParameters
import sttp.tapir.{Schema, Validator}

/** Represents `epochs_parameters` table.
  */
@derive(encoder, decoder)
final case class EpochInfo(
  height: Int,
  storageFeeFactor: Int,
  minValuePerByte: Int,
  maxBlockSize: Int,
  maxBlockCost: Int,
  blockVersion: Byte,
  tokenAccessCost: Int,
  inputCost: Int,
  dataInputCost: Int,
  outputCost: Int
)

object EpochInfo {

  implicit def schema: Schema[EpochInfo]       = Schema.derived[EpochInfo]
  implicit def validator: Validator[EpochInfo] = schema.validator

  def apply(params: EpochParameters): EpochInfo =
    new EpochInfo(
      params.height,
      params.storageFeeFactor,
      params.minValuePerByte,
      params.maxBlockSize,
      params.maxBlockCost,
      params.blockVersion,
      params.tokenAccessCost,
      params.inputCost,
      params.dataInputCost,
      params.outputCost
    )
}
