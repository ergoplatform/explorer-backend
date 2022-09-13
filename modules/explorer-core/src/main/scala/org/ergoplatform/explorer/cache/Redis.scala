package org.ergoplatform.explorer.cache

import cats.effect.{Concurrent, ContextShift, Resource}
import cats.syntax.functor._
import dev.profunktor.redis4cats.RedisCommands
import dev.profunktor.redis4cats.{Redis => RedisI}
import dev.profunktor.redis4cats.connection.{RedisClient, RedisURI}
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.explorer.settings.RedisSettings
import org.ergoplatform.explorer.cache.redisInstances._

object Redis {

  /** Create new Redis client
    */
  def apply[F[_]: Concurrent: ContextShift](
    settings: RedisSettings
  ): Resource[F, RedisCommands[F, String, String]] =
    for {
      implicit0(log: Log[F]) <- Resource.eval(Slf4jLogger.create.map(logInstance(_)))
      uri                    <- Resource.eval(RedisURI.make[F](settings.url))
      client                 <- RedisClient[F].fromUri(uri)
      cmd                    <- RedisI[F].fromClient(client, RedisCodec.Utf8)
    } yield cmd
}
