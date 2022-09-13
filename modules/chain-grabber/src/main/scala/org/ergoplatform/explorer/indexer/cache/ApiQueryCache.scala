package org.ergoplatform.explorer.indexer.cache

import dev.profunktor.redis4cats.RedisCommands

trait ApiQueryCache[F[_]] {
  def flushAll: F[Unit]
}

object ApiQueryCache {

  def make[F[_]](cmd: RedisCommands[F, String, String]): ApiQueryCache[F] =
    new Live[F](cmd)

  final private class Live[F[_]](cmd: RedisCommands[F, String, String]) extends ApiQueryCache[F] {
    def flushAll: F[Unit] = cmd.flushAll
  }
}
