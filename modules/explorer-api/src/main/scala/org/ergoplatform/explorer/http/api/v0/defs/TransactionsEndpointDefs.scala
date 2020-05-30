package org.ergoplatform.explorer.http.api.v0.defs

import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.v0.models.{
  TransactionInfo,
  TransactionSummary,
  TxIdResponse,
  UTransactionInfo
}
import org.ergoplatform.explorer.protocol.ergoInstances._
import sttp.tapir._
import sttp.tapir.json.circe._

object TransactionsEndpointDefs {

  private val PathPrefix = "transactions"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getTxByIdDef :: getUnconfirmedTxByIdDef :: getUnconfirmedTxsDef ::
    getTxsSinceDef :: sendTransactionDef :: Nil

  def getTxByIdDef: Endpoint[TxId, ApiErr, TransactionSummary, Nothing] =
    baseEndpointDef.get
      .in(PathPrefix / path[TxId])
      .out(jsonBody[TransactionSummary])

  def getUnconfirmedTxsDef: Endpoint[Paging, ApiErr, Items[UTransactionInfo], Nothing] =
    baseEndpointDef.get
      .in(PathPrefix / "unconfirmed")
      .in(paging)
      .out(jsonBody[Items[UTransactionInfo]])

  def getUnconfirmedTxByIdDef: Endpoint[TxId, ApiErr, UTransactionInfo, Nothing] =
    baseEndpointDef.get
      .in(PathPrefix / "unconfirmed" / path[TxId])
      .out(jsonBody[UTransactionInfo])

  def getTxsSinceDef: Endpoint[(Paging, Int), ApiErr, List[TransactionInfo], Nothing] =
    baseEndpointDef.get
      .in(paging)
      .in(PathPrefix / "since" / path[Int])
      .out(jsonBody[List[TransactionInfo]])

  def sendTransactionDef: Endpoint[ErgoLikeTransaction, ApiErr, TxIdResponse, Nothing] =
    baseEndpointDef.post
      .in(PathPrefix / "send")
      .in(jsonBody[ErgoLikeTransaction])
      .out(jsonBody[TxIdResponse])
}
