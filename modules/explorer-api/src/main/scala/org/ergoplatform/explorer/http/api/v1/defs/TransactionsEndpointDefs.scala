package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.models.Sorting.SortOrder
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.v1.models.TransactionInfo
import org.ergoplatform.explorer.settings.RequestsSettings
import org.ergoplatform.explorer.{ErgoTreeTemplateHash, TxId}
import sttp.tapir._
import sttp.tapir.json.circe._

final class TransactionsEndpointDefs(settings: RequestsSettings) {

  private val PathPrefix = "transactions"

  def endpoints: List[Endpoint[_, _, _, _]] = getByIdDef :: getByInputsScriptTemplateDef :: Nil

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
}
