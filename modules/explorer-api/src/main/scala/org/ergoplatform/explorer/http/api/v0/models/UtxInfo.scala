package org.ergoplatform.explorer.http.api.v0.models

final case class UtxInfo(
  id: String,
  inputs: List[UtxInputInfo],
  outputs: List[UtxOutputInfo],
  size: Long
)
