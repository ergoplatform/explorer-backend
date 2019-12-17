package org.ergoplatform.explorer.http.api.v0.syntax

import cats.ApplicativeError
import cats.syntax.applicativeError._
import cats.syntax.either._
import cats.syntax.functor._
import org.ergoplatform.explorer.Err

final class ApplicativeThrowOps[
  F[_]: ApplicativeError[*[_], Throwable],
  A
](fa: F[A]) {

  def either: F[Either[Err, A]] =
    fa.map(Either.right[Err, A])
      .handleError {
        case e: Err =>
          Either.left(e)
        case e =>
          Either.left(Err(e.getMessage))
      }
}
