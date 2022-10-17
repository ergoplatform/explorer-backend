package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.v1.models.{ErgoTreeConversionRequest, ErgoTreeHuman}
import sttp.tapir._
import sttp.tapir.json.circe._

final class ErgoTreeEndpointDefs[F[_]]() {

  private val PathPrefix = "ergotree"

  def endpoints: List[Endpoint[_, _, _, _]] = List(convertErgoTreeDef)
  
  def convertErgoTreeDef: Endpoint[ErgoTreeConversionRequest, ApiErr, ErgoTreeHuman, Any] =
    baseEndpointDef.post
      .in(PathPrefix / "convert")
      .in(jsonBody[ErgoTreeConversionRequest])
      .out(jsonBody[ErgoTreeHuman])
}
