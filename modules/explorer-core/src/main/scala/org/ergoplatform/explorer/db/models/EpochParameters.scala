package org.ergoplatform.explorer.db.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.{Schema, Validator}

/** Represents `epochs_parameters` table.
  */
@derive(encoder, decoder)
final case class EpochParameters(
  id: Int,
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

object EpochParameters {

  implicit def schema: Schema[EpochParameters]       = Schema.derive[EpochParameters]
  implicit def validator: Validator[EpochParameters] = Validator.derive[EpochParameters]
}
