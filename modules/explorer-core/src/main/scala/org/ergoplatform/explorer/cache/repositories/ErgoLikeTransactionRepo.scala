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
import org.ergoplatform.explorer.cache.redisInstances._
import org.ergoplatform.explorer.cache.redisTransaction.Transaction
import org.ergoplatform.explorer.settings.UtxCacheSettings
import org.ergoplatform.{ErgoLikeTransaction, JsonCodecs}
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
  ) extends ErgoLikeTransactionRepo[F, Stream]
    with JsonCodecs {

    private val KeyPrefix  = "txs"
    private val CounterKey = "txs:count"

    def put(tx: ErgoLikeTransaction): F[Unit] =
      s"$KeyPrefix:${tx.id}" |> { key =>
        (Logger[F].info(s"Getting counter") >> redis.get(CounterKey).flatTap(x => Logger[F].info(s"Got counter ${x}")) >>= { count =>
          (getCount(count) + 1) |> { newCount =>
            Logger[F].info(s"Inserting new data, count: $count") >>
            redis.set(CounterKey, newCount.toString) >>
              Logger[F].info(s"Settings counter") >>
            redis.append(key, tx.asJson.noSpaces) >>
              Logger[F].info(s"Appending tx") >>
            redis.expire(key, utxCacheSettings.transactionTtl) flatTap(_ => Logger[F].info(s"Done"))
          }
        }) >> Logger[F].info(s"Unconfirmed transaction '${tx.id}' has been cached")
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
        Transaction(redis).run(redis.hDel(KeyPrefix, id), redis.set(CounterKey, (c - 1).toString))
      }) >> Logger[F].debug(s"Transaction '$id' removed from cache")

    def count: F[Int] =
      redis.get(CounterKey).map(getCount)

    private def getCount(raw: Option[String]): Int =
      raw.flatMap(x => Try(x.toInt).toOption).getOrElse(0)
  }
}
