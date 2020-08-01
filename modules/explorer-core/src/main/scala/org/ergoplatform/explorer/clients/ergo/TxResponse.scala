package org.ergoplatform.explorer.clients.ergo

import derevo.circe.decoder
import derevo.derive
import org.ergoplatform.explorer.TxId

/** Node txId response model.
  */
@derive(decoder)
final case class TxResponse(id: TxId)
