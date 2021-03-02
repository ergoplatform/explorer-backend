package org.ergoplatform.explorer.db.models

import derevo.circe.{decoder, encoder}
import derevo.derive

/** Represents `epochs_parameters` table.
  */
@derive(encoder, decoder)
final case class EpochParameters(
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
