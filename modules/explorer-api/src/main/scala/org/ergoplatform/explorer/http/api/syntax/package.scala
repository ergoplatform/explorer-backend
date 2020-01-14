package org.ergoplatform.explorer.http.api

import cats.ApplicativeError

package object syntax {

  object applicativeThrow {

    implicit def toApplicativeThrowOps[
      F[_]: ApplicativeError[*[_], Throwable],
      A
    ](fa: F[A]): ApplicativeThrowOps[F, A] =
      new ApplicativeThrowOps(fa)
  }
}
