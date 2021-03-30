package org.ergoplatform.explorer.db

import cats.effect.{Async, Blocker, ContextShift, Resource, Sync}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.ergoplatform.explorer.settings.DbSettings

object DoobieTrans {

  def apply[F[_]: Async: ContextShift](
    poolName: String,
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
      _ <- Resource.eval(configure(xa)(poolName, settings.cpSize))
    } yield xa

  private def configure[F[_]: Sync](
    xa: HikariTransactor[F]
  )(name: String, maxPoolSize: Int): F[Unit] =
    xa.configure { c =>
      Sync[F].delay {
        c.setAutoCommit(false)
        c.setPoolName(name)
        c.setMaxLifetime(600000)
        c.setIdleTimeout(30000)
        c.setMaximumPoolSize(maxPoolSize)
        c.setMinimumIdle(math.max(2, maxPoolSize / 2))
      }
    }
}
