package org.ergoplatform.explorer.db

import cats.effect.Bracket
import cats.{~>, Monad}
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import fs2.Stream

final case class Trans[D[_], F[_]](xa: D ~> F, xas: Stream[D, *] ~> Stream[F, *])

object Trans {

  def fromDoobie[F[_]: Monad: Bracket[*[_], Throwable]](
    xa: Transactor[F]
  ): ConnectionIO Trans F =
    Trans(xa.trans, xa.transP)
}
