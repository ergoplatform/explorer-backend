package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.models.Sorting.SortOrder
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.v1.models.TokenInfo
import org.ergoplatform.explorer.settings.RequestsSettings
import org.ergoplatform.explorer.{TokenId, TokenSymbol}
import sttp.tapir._
import sttp.tapir.json.circe._

final class TokensEndpointDefs(settings: RequestsSettings) {

  private val PathPrefix = "tokens"

  def endpoints: List[Endpoint[_, _, _, _]] =
    listDef ::
    searchDef ::
    getBySymbolDef ::
    getByIdDef ::
    Nil

  def getByIdDef: Endpoint[TokenId, ApiErr, TokenInfo, Any] =
    baseEndpointDef.get
      .in(PathPrefix / path[TokenId])
      .out(jsonBody[TokenInfo])

  def getBySymbolDef: Endpoint[TokenSymbol, ApiErr, List[TokenInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "bySymbol" / path[TokenSymbol])
      .out(jsonBody[List[TokenInfo]])
      .description("Get all assets with a given Symbol. Note that symbols aren't unique.")

  def searchDef: Endpoint[(String, Paging), ApiErr, Items[TokenInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "search")
      .in(query[String]("query").validate(Validator.minLength(3)))
      .in(paging(settings.maxEntitiesPerRequest))
      .out(jsonBody[Items[TokenInfo]])
      .description("Search by ID or Symbol of an asset. Note that symbols aren't unique.")

  def listDef: Endpoint[(Paging, SortOrder, Boolean), ApiErr, Items[TokenInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix)
      .in(paging(settings.maxEntitiesPerRequest))
      .in(ordering)
      .in(hideNfts)
      .out(jsonBody[Items[TokenInfo]])
}
