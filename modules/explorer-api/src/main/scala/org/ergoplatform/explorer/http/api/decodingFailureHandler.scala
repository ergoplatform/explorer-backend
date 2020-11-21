package org.ergoplatform.explorer.http.api

import cats.effect.{ContextShift, Sync}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.Http4sServerOptions
import sttp.tapir.server.{DecodeFailureHandling, DefaultDecodeFailureHandler, ServerDefaults}

object decodingFailureHandler {

  implicit def customServerOptions[F[_]: Sync: ContextShift]: Http4sServerOptions[F] =
    Http4sServerOptions.default.copy(
      decodeFailureHandler = decodingFailureHandler
    )

  private def decodingFailureResponse(code: StatusCode, message: String): DecodeFailureHandling =
    DecodeFailureHandling.response(
      statusCode(code).and(jsonBody[ApiErr.BadRequest])
    )(ApiErr.badRequest(message))

  private def decodingFailureHandler: DefaultDecodeFailureHandler =
    ServerDefaults.decodeFailureHandler.copy(
      response = decodingFailureResponse
    )
}
