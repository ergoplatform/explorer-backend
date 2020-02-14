package org.ergoplatform.explorer.db

import cats.effect.{Async, Blocker, ContextShift, Resource}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.ergoplatform.explorer.settings.DbSettings

object DbTrans {

  def apply[F[_]: Async: ContextShift](
    settings: DbSettings
  ): Resource[F, HikariTransactor[F]] =
    for {
      cp      <- ExecutionContexts.fixedThreadPool(size = settings.cpSize)
      blocker <- Blocker[F]
      xa <- HikariTransactor.newHikariTransactor[F](
             driverClassName = "org.postgresql.Driver",
             settings.url,
             settings.user,
             settings.pass,
             cp,
             blocker
           )
    } yield xa
}
