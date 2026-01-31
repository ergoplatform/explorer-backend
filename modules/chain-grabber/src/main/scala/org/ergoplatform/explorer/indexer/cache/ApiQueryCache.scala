package org.ergoplatform.explorer.indexer.cache

import cats.implicits._
import dev.profunktor.redis4cats.RedisCommands

trait ApiQueryCache[F[_]] {
  def flushAll: F[Unit]
  
  /** Invalidate cache entries matching the given pattern */
  def invalidatePattern(pattern: String): F[Unit]
  
  /** Invalidate recent blocks starting from given height */
  def invalidateRecentBlocks(fromHeight: Int): F[Unit]
  
  /** Invalidate mutable data (unconfirmed transactions, addresses, stats) */
  def invalidateMutableData(): F[Unit]
}

object ApiQueryCache {

  def make[F[_]](cmd: RedisCommands[F, String, String]): ApiQueryCache[F] =
    new Live[F](cmd)

  final private class Live[F[_]](cmd: RedisCommands[F, String, String]) extends ApiQueryCache[F] {
    
    def flushAll: F[Unit] = cmd.flushAll
    
    def invalidatePattern(pattern: String): F[Unit] =
      cmd.keys(s"ergo.explorer.*$pattern*").flatMap { keys =>
        keys.toList.traverse_(cmd.del)
      }(cmd.F)
    
    def invalidateRecentBlocks(fromHeight: Int): F[Unit] =
      invalidatePattern(s"blocks:height:$fromHeight") *>
      invalidatePattern("blocks:tip")(cmd.F)
    
    def invalidateMutableData(): F[Unit] =
      invalidatePattern("transactions:unconfirmed") *>
      invalidatePattern("addresses:") *>
      invalidatePattern("stats:")(cmd.F)
  }
}
