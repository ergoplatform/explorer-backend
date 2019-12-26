package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Json

final case class BlockExtensionInfo(
  headerId: String,
  digest: String,
  fields: Json
)
