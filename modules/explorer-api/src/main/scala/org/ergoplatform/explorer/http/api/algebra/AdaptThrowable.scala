package org.ergoplatform.explorer.http.api.algebra

import cats.Applicative
import cats.data.EitherT
import cats.syntax.either._
import tofu.HandleTo

/** Adapt effect throwing any [[Throwable]] to some effect throwing narrower error type `E`.
  */
trait AdaptThrowable[F[_], G[_, _], E] {

  def adaptThrowable[A](fa: F[A]): G[E, A]
}

object AdaptThrowable {

  abstract class AdaptThrowableEitherT[F[_], E](
    implicit
    F: Applicative[F],
    H: HandleTo[F, EitherT[F, E, *], Throwable]
  ) extends AdaptThrowable[F, EitherT[F, *, *], E] {

    def adapter: Throwable => E

    final def adaptThrowable[A](fa: F[A]): EitherT[F, E, A] =
      H.handleWith(fa)(e => EitherT(F.pure(adapter(e).asLeft[A])))
  }
}
