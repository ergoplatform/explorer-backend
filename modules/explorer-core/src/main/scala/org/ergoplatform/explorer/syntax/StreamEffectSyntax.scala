package org.ergoplatform.explorer.syntax

trait StreamEffectSyntax {

  implicit final def toStreamEffectOps[F[_], A](fa: F[A]): StreamEffectOps[F, A] =
    new StreamEffectOps(fa)
}

final class StreamEffectOps[F[_], A](fa: F[A]) {

  def asStream: fs2.Stream[F, A] = fs2.Stream.eval(fa)
}
