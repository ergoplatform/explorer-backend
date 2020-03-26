package org.ergoplatform.explorer.clients.ergo

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.ergoplatform.explorer.TxId

/** Node txId response model.
  */
final case class TxResponse(id: TxId)

object TxResponse {

  implicit def decoder: Decoder[TxResponse] = deriveDecoder
}
