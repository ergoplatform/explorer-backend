package org.ergoplatform.explorer.http.api

import cats.effect.{ContextShift, Sync}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.Http4sServerOptions
import sttp.tapir.server.{DecodeFailureHandling, DefaultDecodeFailureHandler, ServerDefaults}

object decodingFailureHandler {

  implicit def myServerOptions[F[_]: Sync: ContextShift]: Http4sServerOptions[F] =
    Http4sServerOptions.default.copy(
      decodeFailureHandler = decodingFailureHandler
    )

  private def decodingFailureResponse(statusCode: StatusCode, message: String): DecodeFailureHandling =
    DecodeFailureHandling.response(
      EndpointOutput.StatusCode().description(statusCode, message).and(jsonBody[ApiErr])
    )((statusCode, ApiErr.badRequest(message)))

  private def decodingFailureHandler: DefaultDecodeFailureHandler =
    ServerDefaults.decodeFailureHandler.copy(
      response = decodingFailureResponse
    )
}
