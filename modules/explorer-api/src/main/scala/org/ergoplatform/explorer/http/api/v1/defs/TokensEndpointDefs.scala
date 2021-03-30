package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.models.Sorting.SortOrder
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.v1.models.TokenInfo
import org.ergoplatform.explorer.settings.RequestsSettings
import sttp.tapir._
import sttp.tapir.json.circe._

final class TokensEndpointDefs(settings: RequestsSettings) {

  private val PathPrefix = "tokens"

  def endpoints: List[Endpoint[_, _, _, _]] =
    listDef ::
    searchByIdDef ::
    getByIdDef ::
    Nil

  def getByIdDef: Endpoint[TokenId, ApiErr, TokenInfo, Any] =
    baseEndpointDef.get
      .in(PathPrefix / path[TokenId])
      .out(jsonBody[TokenInfo])

  def searchByIdDef: Endpoint[(String, Paging), ApiErr, Items[TokenInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "search")
      .in(query[String]("query").validate(Validator.minLength(5)))
      .in(paging(settings.maxEntitiesPerRequest))
      .out(jsonBody[Items[TokenInfo]])

  def listDef: Endpoint[(Paging, SortOrder), ApiErr, Items[TokenInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix)
      .in(paging(settings.maxEntitiesPerRequest))
      .in(ordering)
      .out(jsonBody[Items[TokenInfo]])
}
