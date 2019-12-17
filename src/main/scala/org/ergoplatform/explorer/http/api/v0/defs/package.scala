package org.ergoplatform.explorer.http.api.v0

import io.circe.generic.auto._
import org.ergoplatform.explorer.Err
import sttp.model.StatusCode
import sttp.tapir.json.circe._
import sttp.tapir._

package object defs {

  implicit private val codecErr: CodecForOptional[Err, CodecFormat.Json, _] =
    implicitly[CodecForOptional[Err, CodecFormat.Json, _]]

  val BaseEndpointPrefix: EndpointInput[Unit] = "api" / "v0"

  val baseEndpointDef: Endpoint[Unit, Err, Unit, Nothing] =
    endpoint
      .in(BaseEndpointPrefix)
      .errorOut(
        oneOf(
          statusMapping(
            StatusCode.NotFound,
            jsonBody[Err.NotFound].description("Not found")
          ),
          statusMapping(
            StatusCode.BadRequest,
            jsonBody[Err.BadInput].description("Bad request")
          ),
          statusDefaultMapping(jsonBody[Err].description("Unknown error"))
        )
      )
}
