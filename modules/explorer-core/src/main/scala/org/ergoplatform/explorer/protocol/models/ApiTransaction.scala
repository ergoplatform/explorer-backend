package org.ergoplatform.explorer.protocol.models

import derevo.circe.decoder
import derevo.derive
import org.ergoplatform.explorer.TxId

/** A model mirroring ErgoTransaction entity from Ergo node REST API.
  * See `ErgoTransaction` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
@derive(decoder)
final case class ApiTransaction(
  id: TxId,
  inputs: List[ApiInput],
  dataInputs: List[ApiDataInput],
  outputs: List[ApiOutput],
  size: Int
)
