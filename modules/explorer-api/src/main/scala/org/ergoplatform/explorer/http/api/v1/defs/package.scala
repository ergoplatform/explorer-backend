package org.ergoplatform.explorer.http.api.v1

import org.ergoplatform.explorer.http
import org.ergoplatform.explorer.http.api.ApiErr
import sttp.tapir._

package object defs {

  private val V1Prefix: EndpointInput[Unit] = "api" / "v1"

  val baseEndpointDef: Endpoint[Unit, ApiErr, Unit, Any] =
    http.api.defs.baseEndpointDef(V1Prefix)
}
