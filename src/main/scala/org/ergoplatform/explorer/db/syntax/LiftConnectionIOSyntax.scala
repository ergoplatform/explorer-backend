package org.ergoplatform.explorer.db.syntax

import doobie.free.connection.ConnectionIO
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO

trait LiftConnectionIOSyntax {

  implicit final def toLiftConnectionIOOps[A](
    ca: ConnectionIO[A]
  ): LiftConnectionIOOps[A] =
    new LiftConnectionIOOps(ca)
}

final private[syntax] class LiftConnectionIOOps[A](private val ca: ConnectionIO[A])
  extends AnyVal {

  def liftConnectionIO[F[_]: LiftConnectionIO]: F[A] =
    LiftConnectionIO[F].liftConnectionIO(ca)
}
