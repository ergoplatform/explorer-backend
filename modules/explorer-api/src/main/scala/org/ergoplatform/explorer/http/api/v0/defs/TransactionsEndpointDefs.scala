package org.ergoplatform.explorer.http.api.v0.defs

import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.models.Paging
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.v0.models.{TransactionInfo, UTransactionInfo}
import org.ergoplatform.explorer.protocol.ergoInstances._
import sttp.tapir._
import sttp.tapir.json.circe._

object TransactionsEndpointDefs {

  private val PathPrefix = "transactions"

  def endpoints: List[Endpoint[_, _, _, _]] =
    sendTransactionDef :: getTxByIdDef :: getUnconfirmedTxByIdDef :: getTxsSinceDef :: Nil

  def sendTransactionDef: Endpoint[ErgoLikeTransaction, ApiErr, Unit, Nothing] =
    baseEndpointDef.post.in(PathPrefix / "send").in(jsonBody[ErgoLikeTransaction])

  def getTxByIdDef: Endpoint[TxId, ApiErr, TransactionInfo, Nothing] =
    baseEndpointDef.get
      .in(PathPrefix / path[TxId])
      .out(jsonBody[TransactionInfo])

  def getUnconfirmedTxByIdDef: Endpoint[TxId, ApiErr, UTransactionInfo, Nothing] =
    baseEndpointDef.get
      .in(PathPrefix / "unconfirmed" / path[TxId])
      .out(jsonBody[UTransactionInfo])

  def getTxsSinceDef: Endpoint[(Paging, Int), ApiErr, List[TransactionInfo], Nothing] =
    baseEndpointDef.get
      .in(paging)
      .in(PathPrefix / "since" / path[Int])
      .out(jsonBody[List[TransactionInfo]])
}
