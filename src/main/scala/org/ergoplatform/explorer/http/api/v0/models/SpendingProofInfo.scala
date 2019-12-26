package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Json

final case class SpendingProofInfo(proofBytes: String, extension: Json)
