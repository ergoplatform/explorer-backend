package org.ergoplatform.explorer.protocol.models

import derevo.circe.decoder
import derevo.derive
import org.ergoplatform.explorer.TokenId

/** A model mirroring Asset entity from Ergo node REST API.
  * See `Asset` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
@derive(decoder)
final case class ApiAsset(
  tokenId: TokenId,
  amount: Long
)
