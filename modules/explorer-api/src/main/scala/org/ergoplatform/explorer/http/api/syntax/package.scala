package org.ergoplatform.explorer.http.api

import cats.MonadError
import io.chrisdavenport.log4cats.Logger

package object syntax {

  object applicativeThrow {

    implicit def toApplicativeThrowOps[
      F[_]: MonadError[*[_], Throwable]: Logger,
      A
    ](fa: F[A]): ApplicativeThrowOps[F, A] =
      new ApplicativeThrowOps(fa)
  }
}
