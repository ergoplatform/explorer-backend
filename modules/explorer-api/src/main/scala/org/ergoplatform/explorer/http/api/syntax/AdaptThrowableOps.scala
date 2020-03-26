package org.ergoplatform.explorer.http.api.syntax

import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable

final class AdaptThrowableOps[F[_], G[_, _], E, A](fa: F[A])(
  implicit A: AdaptThrowable[F, G, E]
) {

  def adaptThrowable: G[E, A] = A.adaptThrowable(fa)
}
