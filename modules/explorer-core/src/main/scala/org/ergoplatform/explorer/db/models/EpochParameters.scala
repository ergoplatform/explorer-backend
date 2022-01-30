package org.ergoplatform.explorer.db.models

/** Represents `epochs_parameters` table.
  */
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
