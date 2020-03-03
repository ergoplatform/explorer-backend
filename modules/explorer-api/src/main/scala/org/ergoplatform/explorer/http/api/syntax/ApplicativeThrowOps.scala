package org.ergoplatform.explorer.http.api.syntax

import cats.MonadError
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr

final class ApplicativeThrowOps[
  F[_]: MonadError[*[_], Throwable]: Logger,
  A
](fa: F[A]) {

  def attemptApi: F[Either[ApiErr, A]] =
    fa.map(Either.right[ApiErr, A])
      .handleErrorWith {
        case e: ApiErr =>
          Logger[F].error(e)(e.msg) >> e.asLeft[A].pure[F]
        case e =>
          Logger[F].error(e)(e.getMessage) >> (ApiErr
            .UnknownErr(e.getMessage): ApiErr)
            .asLeft[A]
            .pure[F]
      }
}
