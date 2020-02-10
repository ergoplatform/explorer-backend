package org.ergoplatform.explorer.http.api.v0.defs

import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.models.Paging
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.v0.models.{TransactionInfo, UTransactionInfo}
import sttp.tapir._
import sttp.tapir.json.circe._

object TransactionsEndpointDefs {

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

  def getTxsSinceDef: Endpoint[(Paging, Int), ApiErr, List[TransactionInfo], Nothing] =
    baseEndpointDef
    .in(paging)
    .in(PathPrefix / path[Int])
    .out(jsonBody[List[TransactionInfo]])
}
