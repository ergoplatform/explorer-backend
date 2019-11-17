package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor}

final case class ApiBlockTransactions(
  headerId: String,
  transactions: List[ApiTransaction]
)

object ApiBlockTransactions {

  implicit val decoder: Decoder[ApiBlockTransactions] = { c: HCursor =>
    for {
      headerId     <- c.downField("headerId").as[String]
      transactions <- c.downField("transactions").as[List[ApiTransaction]]
    } yield ApiBlockTransactions(headerId, transactions)
  }
}
