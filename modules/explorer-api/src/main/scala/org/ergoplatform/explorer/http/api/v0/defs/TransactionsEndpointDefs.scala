package org.ergoplatform.explorer.http.api.v0.defs

import cats.data.NonEmptyMap
import cats.syntax.option._
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.explorer.{Address, TxId}
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.models.{Items, Paging, Sorting}
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.v0.models.{
  TransactionInfo,
  TransactionSummary,
  TxIdResponse,
  UTransactionInfo,
  UTransactionSummary
}
import org.ergoplatform.explorer.protocol.ergoInstances._
import sttp.tapir._
import sttp.tapir.json.circe._

object TransactionsEndpointDefs {

  private val PathPrefix = "transactions"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getTxByIdDef :: getUnconfirmedTxByIdDef :: getUnconfirmedTxsByAddressDef ::
    getUnconfirmedTxsDef :: getTxsSinceDef :: sendTransactionDef :: Nil

  def getTxByIdDef: Endpoint[TxId, ApiErr, TransactionSummary, Any] =
    baseEndpointDef.get
      .in(PathPrefix / path[TxId])
      .out(jsonBody[TransactionSummary])

  def getUnconfirmedTxsDef: Endpoint[(Paging, Sorting), ApiErr, Items[UTransactionInfo], Any] =
    baseEndpointDef.get
      .in(paging)
      .in(sorting(allowedSortingFields, defaultFieldOpt = "creationtimestamp".some))
      .in(PathPrefix / "unconfirmed")
      .out(jsonBody[Items[UTransactionInfo]])

  def getUnconfirmedTxByIdDef: Endpoint[TxId, ApiErr, UTransactionSummary, Any] =
    baseEndpointDef.get
      .in(PathPrefix / "unconfirmed" / path[TxId])
      .out(jsonBody[UTransactionSummary])

  def getUnconfirmedTxsByAddressDef: Endpoint[(Paging, Address), ApiErr, Items[UTransactionInfo], Any] =
    baseEndpointDef.get
      .in(paging)
      .in(PathPrefix / "unconfirmed" / "byAddress" / path[Address])
      .out(jsonBody[Items[UTransactionInfo]])

  def getTxsSinceDef: Endpoint[(Paging, Int), ApiErr, List[TransactionInfo], Any] =
    baseEndpointDef.get
      .in(paging)
      .in(PathPrefix / "since" / path[Int])
      .out(jsonBody[List[TransactionInfo]])

  def sendTransactionDef: Endpoint[ErgoLikeTransaction, ApiErr, TxIdResponse, Any] =
    baseEndpointDef.post
      .in(PathPrefix / "send")
      .in(jsonBody[ErgoLikeTransaction])
      .out(jsonBody[TxIdResponse])

  val allowedSortingFields: NonEmptyMap[String, String] =
    NonEmptyMap.of(
      "creationtimestamp" -> "creation_timestamp",
      "size"              -> "size"
    )
}
