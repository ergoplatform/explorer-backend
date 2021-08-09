package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.explorer.Address
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives.paging
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.v0.models.TxIdResponse
import org.ergoplatform.explorer.http.api.v1.models.UTransactionInfo
import org.ergoplatform.explorer.protocol.ergoInstances._
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody

class MempoolEndpointDefs {

  private val PathPrefix = "mempool"

  def endpoints: List[Endpoint[_, _, _, _]] =
    sendTransactionDef :: getTransactionsByAddressDef :: Nil

  def sendTransactionDef: Endpoint[ErgoLikeTransaction, ApiErr, TxIdResponse, Any] =
    baseEndpointDef.post
      .in(PathPrefix / "transactions" / "submit")
      .in(jsonBody[ErgoLikeTransaction])
      .out(jsonBody[TxIdResponse])

  def getTransactionsByAddressDef: Endpoint[(Address, Paging), ApiErr, Items[UTransactionInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "transactions" / "byAddress" / path[Address])
      .in(paging)
      .out(jsonBody[Items[UTransactionInfo]])
}
