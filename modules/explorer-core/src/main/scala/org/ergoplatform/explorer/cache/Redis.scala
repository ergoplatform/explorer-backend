package org.ergoplatform.explorer.cache

import cats.effect.{Concurrent, ContextShift, Resource}
import cats.syntax.functor._
import dev.profunktor.redis4cats.algebra.RedisCommands
import dev.profunktor.redis4cats.connection.{RedisClient, RedisURI}
import dev.profunktor.redis4cats.domain.RedisCodec
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats.interpreter.{Redis => RedisI}
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
      client                 <- RedisClient[F](uri)
      cmd                    <- RedisI[F, String, String](client, RedisCodec.Utf8)
    } yield cmd
}
