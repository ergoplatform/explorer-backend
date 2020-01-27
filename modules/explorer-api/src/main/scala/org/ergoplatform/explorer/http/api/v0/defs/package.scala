package org.ergoplatform.explorer.http.api.v0

import io.circe.generic.auto._
import org.ergoplatform.explorer.http.api.ApiErr
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._

package object defs {

  val BaseEndpointPrefix: EndpointInput[Unit] = "api" / "v0"

  val baseEndpointDef: Endpoint[Unit, ApiErr, Unit, Nothing] =
    endpoint
      .in(BaseEndpointPrefix)
      .errorOut(
        oneOf(
          statusMapping(
            StatusCode.NotFound,
            jsonBody[ApiErr.NotFound].description("Not found")
          ),
          statusMapping(
            StatusCode.BadRequest,
            jsonBody[ApiErr.BadInput].description("Bad request")
          ),
          statusDefaultMapping(jsonBody[ApiErr.UnknownErr].description("Unknown error"))
        )
      ).asInstanceOf[Endpoint[Unit, ApiErr, Unit, Nothing]]
}
