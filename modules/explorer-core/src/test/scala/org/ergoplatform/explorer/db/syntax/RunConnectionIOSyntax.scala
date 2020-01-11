package org.ergoplatform.explorer.db.syntax

import cats.effect.IO
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor

trait RunConnectionIOSyntax {

  implicit def toRunConnectionIOOps[A](ca: ConnectionIO[A]): RunConnectionIOOps[A] =
    new RunConnectionIOOps[A](ca)
}

private[syntax] final class RunConnectionIOOps[A](private val ca: ConnectionIO[A]) extends AnyVal {

  def runWithIO()(implicit xa: Transactor[IO]): A =
    ca.transact(xa).unsafeRunSync()
}
