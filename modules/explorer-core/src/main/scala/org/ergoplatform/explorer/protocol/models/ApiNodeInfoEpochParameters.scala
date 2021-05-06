package org.ergoplatform.explorer.protocol.models

import derevo.circe.{decoder, encoder}
import derevo.derive

@derive(encoder, decoder)
final case class ApiNodeInfoEpochParameters(
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
