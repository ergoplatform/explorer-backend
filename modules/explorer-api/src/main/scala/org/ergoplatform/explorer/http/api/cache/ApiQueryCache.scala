package org.ergoplatform.explorer.http.api.cache

import cats.{Functor, Monad}
import dev.profunktor.redis4cats.RedisCommands
import dev.profunktor.redis4cats.effects.ScriptOutputType
import io.circe.Json
import io.circe.syntax._
import tofu.syntax.monadic._
import tofu.syntax.logging._
import io.circe.parser.parse
import org.ergoplatform.explorer.http.api.cache.models.CachedResponse
import org.ergoplatform.explorer.http.api.cache.types.RequestHash32
import tofu.logging.{Logging, Logs}

import scala.concurrent.duration.FiniteDuration

trait ApiQueryCache[F[_]] {
  def put(key: RequestHash32, value: CachedResponse, ttl: FiniteDuration): F[Unit]

  def get(key: RequestHash32): F[Option[CachedResponse]]
}

object ApiQueryCache {

  def make[I[_] : Functor, F[_] : Monad](implicit cmd: RedisCommands[F, String, String], logs: Logs[I, F]): I[ApiQueryCache[F]] =
    logs.forService[ApiQueryCache[F]].map(implicit __ =>
      new Live[F]
    )

  final private class Live[F[_] : Monad : Logging](implicit val cmd: RedisCommands[F, String, String]) extends ApiQueryCache[F] {
    def put(key: RequestHash32, value: CachedResponse, ttl: FiniteDuration): F[Unit] = {
      val k = mkKey(key)
      info"Going to put key $k into api cache." >>
        cmd.eval(
          """local cur; cur = redis.call("setnx", KEYS[1], ARGV[1]); if cur == 1 then redis.call("expire", KEYS[1], ARGV[2]); end; return cur;""",
          ScriptOutputType.Integer,
          List(k),
          List(value.asJson.noSpaces, ttl.toSeconds.toString)
        ).flatMap { result =>
          info"For key $k set result into api cache is: $result."
        }
    }

    private def mkKey(key: RequestHash32): String =
      s"ergo.explorer.${key.value}"

    def get(key: RequestHash32): F[Option[CachedResponse]] = {
      for {
        _ <- info"Going to put get $key from api cache."
        r <- cmd.get(mkKey(key)).map(_.flatMap(parse(_).toOption)).map { jsonOpt =>
          jsonOpt.flatMap(_.as[CachedResponse].toOption)
        }
        _ <- info"Get key $key result from api cache is $r."
      } yield r

    }
  }
}