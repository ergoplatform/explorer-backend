package org.ergoplatform.explorer.http.api.v0.defs

import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.v0.models.SearchResult
import sttp.tapir._
import sttp.tapir.json.circe._

object SearchEndpointDefs {

  private val PathPrefix = "search"

  def endpoints: List[Endpoint[_, _, _, _]] = Nil

  def searchDef: Endpoint[String, ApiErr, SearchResult, Any] =
    baseEndpointDef.get.in(PathPrefix).in(queryInput).out(jsonBody[SearchResult])

  private def queryInput: EndpointInput.Query[String] =
    query[String]("query").validate(Validator.minLength(5))
}
