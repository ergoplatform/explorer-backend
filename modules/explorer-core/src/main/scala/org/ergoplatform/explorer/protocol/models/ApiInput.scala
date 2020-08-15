package org.ergoplatform.explorer.protocol.models

import derevo.circe.decoder
import derevo.derive
import org.ergoplatform.explorer.BoxId

/** A model mirroring ErgoTransactionInput entity from Ergo node REST API.
  * See `ErgoTransactionInput` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
@derive(decoder)
final case class ApiInput(boxId: BoxId, spendingProof: ApiSpendingProof)
