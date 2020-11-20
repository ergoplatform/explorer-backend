package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.explorer.http.api.ApiErr
import sttp.tapir._

object BoxesEndpointDefs {

  private val PathPrefix = "docs"

  def endpoints: List[Endpoint[_, _, _, _]] = apiSpecDef :: Nil

  def apiSpecDef: Endpoint[Unit, ApiErr, String, Nothing] =
    baseEndpointDef.in(PathPrefix / "openapi").out(plainBody[String])
}
