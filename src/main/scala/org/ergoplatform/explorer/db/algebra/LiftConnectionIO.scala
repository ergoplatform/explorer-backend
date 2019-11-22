package org.ergoplatform.explorer.db.algebra

import cats.~>
import doobie.free.connection.ConnectionIO
import simulacrum.typeclass

@typeclass
trait LiftConnectionIO[F[_]] {
  def liftConnectionIO[A](ca: ConnectionIO[A]): F[A]
  def liftConnectionIOK: ConnectionIO ~> F = λ[ConnectionIO ~> F](liftConnectionIO(_))
}

object LiftConnectionIO {

  implicit val CIOLiftConnectionIO: LiftConnectionIO[ConnectionIO] =
    new LiftConnectionIO[ConnectionIO] {
      def liftConnectionIO[A](ca: ConnectionIO[A]): ConnectionIO[A] = ca
    }
}
