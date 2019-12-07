package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor}
import org.ergoplatform.explorer.Id

/** A model mirroring BlockTransactions entity from Ergo node REST API.
  * See `BlockTransactions` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
final case class ApiBlockTransactions(
  headerId: Id,
  transactions: List[ApiTransaction]
)

object ApiBlockTransactions {

  implicit val decoder: Decoder[ApiBlockTransactions] = { c: HCursor =>
    for {
      headerId     <- c.downField("headerId").as[Id]
      transactions <- c.downField("transactions").as[List[ApiTransaction]]
    } yield ApiBlockTransactions(headerId, transactions)
  }
}
