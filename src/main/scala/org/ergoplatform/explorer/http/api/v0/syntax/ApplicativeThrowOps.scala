package org.ergoplatform.explorer.http.api.v0.syntax

import cats.ApplicativeError
import cats.syntax.applicativeError._
import cats.syntax.either._
import cats.syntax.functor._
import org.ergoplatform.explorer.Err.ApiErr

final class ApplicativeThrowOps[
  F[_]: ApplicativeError[*[_], Throwable],
  A
](fa: F[A]) {

  def either: F[Either[ApiErr, A]] =
    fa.map(Either.right[ApiErr, A])
      .handleError {
        case e: ApiErr =>
          Either.left(e)
        case e =>
          Either.left(ApiErr.UnknownErr(e.getMessage))
      }
}
