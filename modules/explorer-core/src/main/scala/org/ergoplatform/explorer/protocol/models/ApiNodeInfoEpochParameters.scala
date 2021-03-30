package org.ergoplatform.explorer.protocol.models

import derevo.circe.decoder
import derevo.derive

@derive(decoder)
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
