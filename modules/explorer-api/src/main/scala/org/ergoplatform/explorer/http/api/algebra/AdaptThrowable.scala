package org.ergoplatform.explorer.http.api.algebra

import cats.MonadError
import cats.data.EitherT

/** Adapt effect which can fail with any [[Throwable]] to some effect with narrower static error type `E`.
  */
trait AdaptThrowable[F[_], G[_, _], E] {

  def adaptThrowable[A](fa: F[A]): G[E, A]
}

object AdaptThrowable {

  abstract class AdaptThrowableEitherT[F[_], E](
    implicit F: MonadError[F, Throwable]
  ) extends AdaptThrowable[F, EitherT[F, *, *], E] {

    def adapter: Throwable => F[E]

    final def adaptThrowable[A](fa: F[A]): EitherT[F, E, A] =
      EitherT(F.attempt(fa)).leftSemiflatMap(adapter)
  }
}
