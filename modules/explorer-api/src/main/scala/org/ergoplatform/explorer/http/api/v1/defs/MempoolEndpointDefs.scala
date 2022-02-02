package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.explorer.Address
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives.{limit, paging}
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.v0.models.TxIdResponse
import org.ergoplatform.explorer.http.api.v1.models.{OutputInfo, UTransactionInfo}
import org.ergoplatform.explorer.protocol.ergoInstances._
import org.ergoplatform.explorer.settings.RequestsSettings
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody

final class MempoolEndpointDefs[F[_]] {

  private val PathPrefix = "mempool"

  def endpoints: List[Endpoint[_, _, _, _]] =
    sendTransactionDef :: getTransactionsByAddressDef :: streamUnspentOutputsDef :: Nil

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

  def streamUnspentOutputsDef: Endpoint[Unit, ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "boxes" / "unspent")
      .out(streamBody(Fs2Streams[F])(Schema.derived[List[OutputInfo]], CodecFormat.Json(), None))
      .description("Get a stream of unspent outputs")
}
