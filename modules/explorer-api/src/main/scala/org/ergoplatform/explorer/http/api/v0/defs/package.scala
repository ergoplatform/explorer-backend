package org.ergoplatform.explorer.http.api.v0

import org.ergoplatform.explorer.http
import org.ergoplatform.explorer.http.api.ApiErr
import sttp.tapir._

package object defs {

  private val V0Prefix: EndpointInput[Unit] = "api" / "v0"

  val baseEndpointDef: Endpoint[Unit, ApiErr, Unit, Any] =
    http.api.defs.baseEndpointDef(V0Prefix)
}
