package org.ergoplatform.explorer.db.algebra

import cats.~>
import doobie.free.connection.ConnectionIO
import simulacrum.typeclass

/** A type class allowing to lift [[ConnectionIO]] into some effect `F[_]`.
  */
trait LiftConnectionIO[F[_]] {

  def liftConnectionIO[A](ca: ConnectionIO[A]): F[A]

  // see: https://github.com/typelevel/kind-projector#polymorphic-lambda-values
  def liftConnectionIOK: ConnectionIO ~> F = λ[ConnectionIO ~> F](liftConnectionIO(_))
}

object LiftConnectionIO {

  def apply[F[_]](implicit ev: LiftConnectionIO[F]): LiftConnectionIO[F] = ev

  implicit val CIOLiftConnectionIO: LiftConnectionIO[ConnectionIO] =
    new LiftConnectionIO[ConnectionIO] {
      def liftConnectionIO[A](ca: ConnectionIO[A]): ConnectionIO[A] = ca
    }
}
