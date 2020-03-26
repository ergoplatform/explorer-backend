package org.ergoplatform.explorer.cache

import dev.profunktor.redis4cats.effect.Log
import io.chrisdavenport.log4cats.Logger

object redisInstances {

  implicit def logInstance[F[_]](implicit logger: Logger[F]): Log[F] =
    new Log[F] {
      def info(msg: => String): F[Unit]  = logger.info(msg)
      def error(msg: => String): F[Unit] = logger.error(msg)
    }
}
