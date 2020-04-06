package org.ergoplatform.explorer.http.api

import cats.ApplicativeError
import cats.syntax.applicative._
import io.circe.DecodingFailure
import org.ergoplatform.explorer.http.api.algebra.HttpErrorHandler.RoutesHttpErrorHandler
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder._

object httpErrorHandler {

  implicit def handlerThrowable[F[_]](
    implicit A: ApplicativeError[F, Throwable]
  ): RoutesHttpErrorHandler[F, Throwable] =
    new RoutesHttpErrorHandler[F, Throwable] {

      override def handler: Throwable => F[Response[F]] = {
        case e: ApiErr =>
          Response[F](Status.fromInt(e.status).getOrElse(Status.InternalServerError))
            .withEntity(e)
            .pure
        case e: DecodingFailure =>
          Response[F](Status.BadRequest)
            .withEntity(ApiErr.BadRequest(e.message): ApiErr)
            .pure
        case e =>
          Response[F](Status.InternalServerError)
            .withEntity(ApiErr.UnknownErr(e.getMessage): ApiErr)
            .pure
      }
    }
}
