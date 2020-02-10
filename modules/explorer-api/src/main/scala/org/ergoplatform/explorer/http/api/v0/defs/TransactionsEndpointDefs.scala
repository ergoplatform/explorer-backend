package org.ergoplatform.explorer.http.api.v0.defs

import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.v0.models.{TransactionInfo, UTransactionInfo}
import sttp.tapir._
import sttp.tapir.json.circe._

class TransactionsEndpointDefs {

  private val PathPrefix = "addresses"

  def endpoints: List[Endpoint[_, _, _, _]] = Nil

  def getTxByIdDef: Endpoint[TxId, ApiErr, TransactionInfo, Nothing] =
    baseEndpointDef
    .in(PathPrefix / path[TxId])
    .out(jsonBody[TransactionInfo])

  def getUnconfirmedTxByIdDef: Endpoint[TxId, ApiErr, UTransactionInfo, Nothing] =
    baseEndpointDef
      .in(PathPrefix / path[TxId])
      .out(jsonBody[UTransactionInfo])
}
