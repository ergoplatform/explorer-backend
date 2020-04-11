package org.ergoplatform.explorer.http.api.syntax

import cats.Monad
import cats.data.EitherT
import cats.syntax.applicative._
import org.ergoplatform.explorer.http.api.ApiErr

final class RoutesOps[F[_], A](val fa: EitherT[F, ApiErr, Option[A]]) extends AnyVal {

  def orNotFound(what: String)(implicit M: Monad[F]): EitherT[F, ApiErr, A] =
    fa.flatMap(
      _.fold[EitherT[F, ApiErr, A]](EitherT.left((ApiErr.notFound(what): ApiErr).pure))(
        EitherT.pure[F, ApiErr](_)
      )
    )
}
