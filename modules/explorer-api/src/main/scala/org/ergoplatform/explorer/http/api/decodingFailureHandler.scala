package org.ergoplatform.explorer.http.api

import cats.effect.{ContextShift, Sync}
import sttp.model.{Header, StatusCode}
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.Http4sServerOptions
import sttp.tapir.server.interceptor.ValuedEndpointOutput
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler
import cats.syntax.option._

object decodingFailureHandler {

  //todo: check
  implicit def customServerOptions[F[_]: Sync: ContextShift]: Http4sServerOptions[F, F] =
    Http4sServerOptions.customInterceptors(
      decodeFailureHandler = decodingFailureHandler,
      exceptionHandler = none,
      serverLog = none
    )

  private def decodingFailureResponse(c: StatusCode, hs: List[Header], m: String): ValuedEndpointOutput[_] =
    ValuedEndpointOutput(statusCode.and(headers).and(jsonBody[ApiErr.BadRequest]), (c, hs, ApiErr.badRequest(m)))

  private def decodingFailureHandler: DefaultDecodeFailureHandler =
    DefaultDecodeFailureHandler.handler.copy(
      response = decodingFailureResponse
    )
}
