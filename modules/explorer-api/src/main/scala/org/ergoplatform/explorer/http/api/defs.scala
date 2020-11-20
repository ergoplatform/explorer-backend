package org.ergoplatform.explorer.http.api

import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._

object defs {

  def baseEndpointDef(basePrefix: EndpointInput[Unit]): Endpoint[Unit, ApiErr, Unit, Nothing] =
    endpoint
      .in(basePrefix)
      .errorOut(
        oneOf(
          statusMapping(
            StatusCode.NotFound,
            jsonBody[ApiErr.NotFound].description("Not found")
          ),
          statusMapping(
            StatusCode.BadRequest,
            jsonBody[ApiErr.BadRequest].description("Bad request")
          ),
          statusDefaultMapping(jsonBody[ApiErr.UnknownErr].description("Unknown error"))
        )
      )
      .asInstanceOf[Endpoint[Unit, ApiErr, Unit, Nothing]]
}
