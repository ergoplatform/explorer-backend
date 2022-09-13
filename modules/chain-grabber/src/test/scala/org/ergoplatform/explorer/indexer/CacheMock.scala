package org.ergoplatform.explorer.indexer

import cats.Applicative
import org.ergoplatform.explorer.indexer.cache.ApiQueryCache

object CacheMock {
  def make[F[_]: Applicative]: ApiQueryCache[F] = new ApiQueryCache[F] {
    def flushAll: F[Unit] = Applicative[F].unit
  }
}
