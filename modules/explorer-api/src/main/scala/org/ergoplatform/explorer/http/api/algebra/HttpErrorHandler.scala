package org.ergoplatform.explorer.http.api.algebra

import cats.ApplicativeError
import cats.data.{Kleisli, OptionT}
import org.http4s._
import org.http4s.dsl.Http4sDsl

/** A type class allowing to handle specific http error (error family).
  */
trait HttpErrorHandler[F[_], E] {

  def handle(routes: HttpRoutes[F]): HttpRoutes[F]
}

object HttpErrorHandler {

  abstract class RoutesHttpErrorHandler[F[_], E](
    implicit A: ApplicativeError[F, E]
  ) extends HttpErrorHandler[F, E]
    with Http4sDsl[F] {

    def handler: E => F[Response[F]]

    def handle(routes: HttpRoutes[F]): HttpRoutes[F] =
      Kleisli { req =>
        OptionT {
          A.handleErrorWith(routes.run(req).value)(e => A.map(handler(e))(Option(_)))
        }
      }
  }

  def apply[F[_], E](
    implicit ev: HttpErrorHandler[F, E]
  ): HttpErrorHandler[F, E] = ev
}
