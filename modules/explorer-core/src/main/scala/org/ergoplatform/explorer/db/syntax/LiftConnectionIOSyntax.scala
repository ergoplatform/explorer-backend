package org.ergoplatform.explorer.db.syntax

import doobie.free.connection.ConnectionIO
import org.ergoplatform.explorer.LiftConnectionIO

trait LiftConnectionIOSyntax {

  implicit final def toLiftConnectionIOOps[A](
    ca: ConnectionIO[A]
  ): LiftConnectionIOOps[A] =
    new LiftConnectionIOOps(ca)
}

final private[syntax] class LiftConnectionIOOps[A](private val ca: ConnectionIO[A])
  extends AnyVal {

  def liftConnIO[F[_]: LiftConnectionIO]: F[A] =
    implicitly[LiftConnectionIO[F]].liftF(ca)
}
