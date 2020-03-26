package org.ergoplatform.explorer.http.api

import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable

package object syntax {

  object adaptThrowable {

    implicit def toAdaptThrowableOps[F[_], G[_, _], E, A](fa: F[A])(
      implicit A: AdaptThrowable[F, G, E]
    ): AdaptThrowableOps[F, G, E, A] =
      new AdaptThrowableOps(fa)
  }
}
