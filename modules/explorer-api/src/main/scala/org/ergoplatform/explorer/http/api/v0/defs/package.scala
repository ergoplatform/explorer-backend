package org.ergoplatform.explorer.http.api.v0

import org.ergoplatform.explorer.http.api.ApiErr
import sttp.tapir._
import sttp.tapir.json.circe._

package object defs {

  val BaseEndpointPrefix: EndpointInput[Unit] = "api" / "v0"

  val baseEndpointDef: Endpoint[Unit, ApiErr, Unit, Nothing] =
    endpoint
      .in(BaseEndpointPrefix)
      .errorOut(
        jsonBody[ApiErr].description("API error")
      )
}
