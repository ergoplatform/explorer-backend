package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Json

final case class OutputInfo(
  id: String,
  value: Long,
  creationHeight: Int,
  ergoTree: String,
  address: String,
  assets: Seq[AssetInfo],
  additionalRegisters: Json,
  spentTxIs: Option[String],
  mainChain: Boolean
)
