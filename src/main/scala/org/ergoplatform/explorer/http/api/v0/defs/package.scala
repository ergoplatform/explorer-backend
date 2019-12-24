package org.ergoplatform.explorer.http.api.v0

import io.circe.generic.auto._
import org.ergoplatform.explorer.Err.ApiErr
import sttp.model.StatusCode
import sttp.tapir.json.circe._
import sttp.tapir._

package object defs {

  implicit private val codecErr: CodecForOptional[ApiErr, CodecFormat.Json, _] =
    implicitly[CodecForOptional[ApiErr, CodecFormat.Json, _]]

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
          statusDefaultMapping(jsonBody[ApiErr].description("Unknown error"))
        )
      )
}
