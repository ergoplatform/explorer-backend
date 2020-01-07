package org.ergoplatform.explorer.algebra

import cats.ApplicativeError
import cats.syntax.applicativeError._

/** A type class allowing to signal business errors of type `E`.
  */
trait Raise[F[_], E <: Throwable] {

  def raise[A](e: E): F[A]
}

object Raise {

  def apply[F[_], E <: Throwable](
    implicit ev: Raise[F, E]
  ): Raise[F, E] = ev

  implicit def instance[
    F[_]: ApplicativeError[*[_], Throwable],
    E <: Throwable
  ]: Raise[F, E] =
    new Raise[F, E] {
      def raise[A](e: E): F[A] = e.raiseError
    }

  object syntax {

    implicit class RaiseOps[
      F[_]: Raise[*[_], E],
      E <: Throwable
    ](e: E) {
      def raise[A]: F[A] = Raise[F, E].raise[A](e)
    }
  }
}
