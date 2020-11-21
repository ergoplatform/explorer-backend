package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.explorer.http.api.ApiErr
import sttp.tapir._

object DocsEndpointDefs {

  private val PathPrefix = "docs"

  def endpoints: List[Endpoint[_, _, _, _]] = apiSpecDef :: Nil

  def apiSpecDef: Endpoint[Unit, ApiErr, String, Any] =
    baseEndpointDef.in(PathPrefix / "openapi").out(plainBody[String])
}
