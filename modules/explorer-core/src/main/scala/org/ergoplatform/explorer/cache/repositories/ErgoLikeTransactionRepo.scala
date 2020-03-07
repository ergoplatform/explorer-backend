package org.ergoplatform.explorer.cache.repositories

import cats.effect.Concurrent
import cats.syntax.functor._
import cats.syntax.either._
import cats.instances.list._
import mouse.any._
import io.circe.syntax._
import io.circe.parser._
import dev.profunktor.redis4cats.algebra.RedisCommands
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.explorer.CRaise
import org.ergoplatform.explorer.Err.UtxBroadcastingErr.TxDeserializationFailed
import org.ergoplatform.{ErgoLikeTransaction, JsonCodecs}
import org.ergoplatform.explorer.settings.UtxCacheSettings
import org.ergoplatform.explorer.cache.redisInstances._
import tofu.syntax.raise._
import fs2.Stream
import org.ergoplatform.explorer.cache.redisTransaction.Transaction
import scorex.util.ModifierId

trait ErgoLikeTransactionRepo[F[_], S[_[_], _]] {

  def put(tx: ErgoLikeTransaction): F[Unit]

  def getAll: S[F, ErgoLikeTransaction]

  def delete(id: ModifierId): F[Unit]
}

object ErgoLikeTransactionRepo {

  def apply[F[_]: Concurrent](
    utxCacheSettings: UtxCacheSettings,
    redis: RedisCommands[F, String, String]
  ): F[ErgoLikeTransactionRepo[F, Stream]] =
    Slf4jLogger.create.map { implicit logger =>
      new Live[F](utxCacheSettings, redis)
    }

  final private class Live[
    F[_]: Concurrent: Logger: CRaise[*[_], TxDeserializationFailed]
  ](
    utxCacheSettings: UtxCacheSettings,
    redis: RedisCommands[F, String, String]
  ) extends ErgoLikeTransactionRepo[F, Stream]
    with JsonCodecs {

    private val KeyPrefix = "txs"

    def put(tx: ErgoLikeTransaction): F[Unit] =
      s"$KeyPrefix:${tx.id}" |> { key =>
        Transaction(redis).run(
          redis.append(key, tx.asJson.noSpaces),
          redis.expire(key, utxCacheSettings.transactionTtl)
        )
      }

    def getAll: Stream[F, ErgoLikeTransaction] =
      Stream
        .evals(redis.keys(s"$KeyPrefix:*"))
        .chunkN(100)
        .flatMap { ids =>
          Stream.evals(redis.mGet(ids.toList.toSet).map(_.values.toList))
        }
        .evalMap(rawTx =>
          parse(rawTx)
            .flatMap(_.as[ErgoLikeTransaction])
            .leftMap(_ => TxDeserializationFailed(rawTx))
            .toRaise[F]
        )

    def delete(id: ModifierId): F[Unit] =
      redis.hDel(KeyPrefix, id)
  }
}
