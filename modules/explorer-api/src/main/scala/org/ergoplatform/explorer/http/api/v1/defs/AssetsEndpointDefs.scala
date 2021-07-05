package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.models.Sorting.SortOrder
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.v1.models.{AssetInfo, TokenInfo}
import org.ergoplatform.explorer.settings.RequestsSettings
import sttp.tapir._
import sttp.tapir.json.circe._

final class AssetsEndpointDefs(settings: RequestsSettings) {

  private val PathPrefix = "assets"

  def endpoints: List[Endpoint[_, _, _, _]] =
    listTokensDef ::
    searchByTokenIdDef ::
    Nil

  def searchByTokenIdDef: Endpoint[(String, Paging), ApiErr, Items[AssetInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "search" / "byTokenId")
      .in(query[String]("query").validate(Validator.minLength(5)))
      .in(paging(settings.maxEntitiesPerRequest))
      .out(jsonBody[Items[AssetInfo]])

  def listTokensDef: Endpoint[(Paging, SortOrder, Boolean), ApiErr, Items[TokenInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix)
      .in(paging(settings.maxEntitiesPerRequest))
      .in(ordering)
      .in(hideNfts)
      .out(jsonBody[Items[TokenInfo]])
      .description("Use '/tokens' instead")
      .deprecated()
}
