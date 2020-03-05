package org.ergoplatform.explorer.cache.repositories

import dev.profunktor.redis4cats.algebra.RedisCommands
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.explorer.settings.UtxCacheSettings

trait ErgoLikeTransactionRepo[F[_], S[_[_], _]] {

  def put(tx: ErgoLikeTransaction): F[Unit]

  def getAll: S[F, ErgoLikeTransaction]
}

object ErgoLikeTransactionRepo {

  def apply[F[_]](
    utxCacheSettings: UtxCacheSettings,
    redis: RedisCommands[F, String, String]
  ): ErgoLikeTransactionRepo[F, fs2.Stream] =
    new Live[F](utxCacheSettings, redis)

  final private class Live[F[_]](
    utxCacheSettings: UtxCacheSettings,
    redis: RedisCommands[F, String, String]
  ) extends ErgoLikeTransactionRepo[F, fs2.Stream] {

    def put(tx: ErgoLikeTransaction): F[Unit] = ???

    def getAll: fs2.Stream[F, ErgoLikeTransaction] = ???
  }
}
