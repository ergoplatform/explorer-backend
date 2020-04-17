package org.ergoplatform.explorer.cache.repositories

import cats.effect.Concurrent
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.functor._
import dev.profunktor.redis4cats.algebra.RedisCommands
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.parser._
import io.circe.syntax._
import mouse.any._
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.explorer.cache.redisTransaction.Transaction
import org.ergoplatform.explorer.settings.UtxCacheSettings
import org.ergoplatform.explorer.protocol.ergoInstances._
import scorex.util.ModifierId

import scala.util.Try

trait ErgoLikeTransactionRepo[F[_], S[_[_], _]] {

  def put(tx: ErgoLikeTransaction): F[Unit]

  def getAll: S[F, ErgoLikeTransaction]

  def delete(id: ModifierId): F[Unit]

  def count: F[Int]
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
    F[_]: Concurrent: Logger
  ](
    utxCacheSettings: UtxCacheSettings,
    redis: RedisCommands[F, String, String]
  ) extends ErgoLikeTransactionRepo[F, Stream] {

    private val KeyPrefix  = "txs"
    private val CounterKey = "txs:count"

    def put(tx: ErgoLikeTransaction): F[Unit] =
      s"$KeyPrefix:${tx.id}" |> { key =>
        redis.get(key).flatMap {
          case None =>
            (redis.get(CounterKey) >>= { count =>
              (getCount(count) + 1) |> { newCount =>
                redis.set(CounterKey, newCount.toString) >>
                redis.append(key, tx.asJson.noSpaces) >>
                redis.expire(key, utxCacheSettings.transactionTtl)
              }
            }) >> Logger[F].info(s"Unconfirmed transaction '${tx.id}' has been cached")
          case _ =>
            Logger[F].debug(s"An attempt to persist transaction '${tx.id}' twice")
        }
      }

    def getAll: Stream[F, ErgoLikeTransaction] =
      Stream
        .evals(redis.keys(s"$KeyPrefix:*"))
        .chunkN(100)
        .flatMap { ids =>
          Stream.evals(redis.mGet(ids.toList.toSet).map(_.values.toList))
        }
        .map(rawTx =>
          parse(rawTx)
            .flatMap(_.as[ErgoLikeTransaction])
            .toOption
        )
        .unNone

    def delete(id: ModifierId): F[Unit] =
      (count >>= { c =>
        redis.del(s"$KeyPrefix:$id") >> redis.set(CounterKey, (c - 1).toString)
      }) >> Logger[F].debug(s"Transaction '$id' removed from cache")

    def count: F[Int] =
      redis.get(CounterKey).map(getCount)

    private def getCount(raw: Option[String]): Int =
      raw.flatMap(x => Try(x.toInt).toOption).getOrElse(0)
  }
}
