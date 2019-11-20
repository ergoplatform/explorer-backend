package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor}
import org.ergoplatform.explorer.Id

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
