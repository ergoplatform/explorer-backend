package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.models.Sorting.SortOrder
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.v1.models.{OutputInfo, TransactionInfo}
import org.ergoplatform.explorer.settings.RequestsSettings
import org.ergoplatform.explorer.{ErgoTreeTemplateHash, TxId}
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir._
import sttp.tapir.json.circe._

final class TransactionsEndpointDefs[F[_]](settings: RequestsSettings) {

  private val PathPrefix = "transactions"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getByIdDef ::
    getByInputsScriptTemplateDef ::
    streamByGixDef ::
    Nil

  def getByIdDef: Endpoint[TxId, ApiErr, TransactionInfo, Any] =
    baseEndpointDef.get
      .in(PathPrefix / path[TxId])
      .out(jsonBody[TransactionInfo])

  def getByInputsScriptTemplateDef
    : Endpoint[(ErgoTreeTemplateHash, Paging, SortOrder), ApiErr, Items[TransactionInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "byInputsScriptTemplateHash" / path[ErgoTreeTemplateHash])
      .in(paging(settings.maxEntitiesPerRequest))
      .in(ordering)
      .out(jsonBody[Items[TransactionInfo]])

  def streamByGixDef: Endpoint[(Long, Int), ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "byGlobalIndex" / "stream")
      .in(query[Long]("minGix").validate(Validator.min(0L)).description("Min global index (in blockchain) of the TX"))
      .in(limit(settings.maxEntitiesPerRequest))
      .out(streamBody(Fs2Streams[F])(Schema.derived[List[TransactionInfo]], CodecFormat.Json(), None))
      .description("Get a stream of transactions ordered by global index")
}
