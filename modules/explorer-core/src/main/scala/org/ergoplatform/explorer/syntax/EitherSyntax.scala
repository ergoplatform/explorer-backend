package org.ergoplatform.explorer.syntax

import cats.Applicative
import org.ergoplatform.explorer.algebra.Raise
import org.ergoplatform.explorer.syntax.EitherSyntax.EitherOps

trait EitherSyntax {

  implicit final def toEitherOps[E, A](either: E Either A): EitherOps[E, A] =
    new EitherOps(either)
}

object EitherSyntax {

  final private[syntax] class EitherOps[E, A](either: E Either A) {

    def liftToRaise[F[_]](
      implicit
      F: Raise[F, _ >: E],
      A: Applicative[F]
    ): F[A] =
      either match {
        case Right(value) => A.pure(value)
        case Left(e)      => F.raise(e)
      }
  }
}
