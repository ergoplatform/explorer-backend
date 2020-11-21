package org.ergoplatform.explorer.http.api.v0.defs

import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives.paging
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.v0.models.OutputInfo
import sttp.tapir._
import sttp.tapir.json.circe._

object AssetsEndpointDefs {

  private val PathPrefix = "assets"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getAllIssuingBoxesDef :: getIssuingBoxDef :: Nil

  def getAllIssuingBoxesDef: Endpoint[Paging, ApiErr, Items[OutputInfo], Any] =
    baseEndpointDef
      .in(paging)
      .in(PathPrefix / "issuingBoxes")
      .out(jsonBody[Items[OutputInfo]])

  def getIssuingBoxDef: Endpoint[TokenId, ApiErr, List[OutputInfo], Any] =
    baseEndpointDef
      .in(PathPrefix / path[TokenId] / "issuingBox")
      .out(jsonBody[List[OutputInfo]])
}
