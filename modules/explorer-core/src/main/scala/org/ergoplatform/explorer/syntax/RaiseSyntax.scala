package org.ergoplatform.explorer.syntax

import org.ergoplatform.explorer.algebra.Raise
import org.ergoplatform.explorer.syntax.RaiseSyntax.RaiseOps

trait RaiseSyntax {

  implicit final def toRaiseOps[E](e: E): RaiseOps[E] =
    new RaiseOps(e)
}

object RaiseSyntax {

  final private[syntax] class RaiseOps[E](private val e: E) extends AnyVal {

    def raise[F[_], A](implicit F: Raise[F, _ >: E]): F[A] =
      F.raise(e)
  }
}
