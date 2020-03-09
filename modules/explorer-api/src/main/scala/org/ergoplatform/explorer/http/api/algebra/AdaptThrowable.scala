package org.ergoplatform.explorer.http.api.algebra

import cats.ApplicativeError
import cats.data.EitherT

/** Adapt effect throwing any [[Throwable]] to some effect throwing narrower error type `E`.
  */
trait AdaptThrowable[F[_], G[_, _], E] {

  def adaptThrowable[A](fa: F[A]): G[E, A]
}

object AdaptThrowable {

  abstract class AdaptThrowableEitherT[F[_], E](
    implicit A: ApplicativeError[F, Throwable]
  ) extends AdaptThrowable[F, EitherT[F, *, *], E] {

    def adapter: Throwable => E

    final def adaptThrowable[A](fa: F[A]): EitherT[F, E, A] =
      EitherT(A.attempt(fa)).leftMap(adapter)
  }
}
