package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Json

final case class UtxOutputInfo(
  boxId: String,
  value: Long,
  creationHeight: Int,
  ergoTree: String,
  assets: List[AssetInfo],
  additionalRegisters: Json
)
