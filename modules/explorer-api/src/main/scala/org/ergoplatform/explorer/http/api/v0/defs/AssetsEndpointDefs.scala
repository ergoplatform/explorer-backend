package org.ergoplatform.explorer.http.api.v0.defs

import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.v0.models.OutputInfo
import sttp.tapir._
import sttp.tapir.json.circe._

object AssetsEndpointDefs {

  private val PathPrefix = "assets"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getAllIssuingBoxesDef :: Nil

  def getAllIssuingBoxesDef: Endpoint[Unit, ApiErr, List[OutputInfo], Nothing] =
    baseEndpointDef
      .in(PathPrefix / "issuingBoxes")
      .out(jsonBody[List[OutputInfo]])
}
