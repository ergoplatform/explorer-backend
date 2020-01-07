package org.ergoplatform.explorer.syntax

import cats.Applicative
import org.ergoplatform.explorer.algebra.Raise
import org.ergoplatform.explorer.syntax.OptionSyntax.OptionOps

trait OptionSyntax {

  implicit final def toOptionOps[A](oa: Option[A]): OptionOps[A] =
    new OptionOps(oa)
}

object OptionSyntax {

  final private[syntax] class OptionOps[A](private val oa: Option[A]) extends AnyVal {

    def liftTo[F[_]] = new LiftToPartiallyApplied[F, A](oa)
  }

  final private[syntax] class LiftToPartiallyApplied[F[_], A](oa: Option[A]) {

    def apply[E](ifEmpty: => E)(
      implicit
      F: Raise[F, _ >: E],
      A: Applicative[F]
    ): F[A] =
      oa match {
        case Some(value) => A.pure(value)
        case None        => F.raise(ifEmpty)
      }
  }
}
