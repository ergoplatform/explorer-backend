package org.ergoplatform.explorer.http.api.cache

import cats.Monad
import cats.effect.Sync
import dev.profunktor.redis4cats.RedisCommands
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.parser.parse
import io.circe.syntax._
import org.ergoplatform.explorer.http.api.cache.models.CachedResponse
import org.ergoplatform.explorer.http.api.cache.types.RequestHash32
import tofu.syntax.monadic._

import scala.concurrent.duration.FiniteDuration

trait ApiQueryCache[F[_]] {
  def put(key: RequestHash32, value: CachedResponse, ttl: FiniteDuration): F[Unit]

  def get(key: RequestHash32): F[Option[CachedResponse]]
}

object ApiQueryCache {

  def make[F[_]: Sync](cmd: RedisCommands[F, String, String]): F[ApiQueryCache[F]] =
    Slf4jLogger
      .create[F]
      .map { implicit __ =>
        new Live[F](cmd)
      }

  final private class Live[F[_]: Monad: Logger](val cmd: RedisCommands[F, String, String]) extends ApiQueryCache[F] {

    def put(key: RequestHash32, value: CachedResponse, ttl: FiniteDuration): F[Unit] = {
      val k = mkKey(key)
      Logger[F].info(s"Going to put key $k into api cache.") >>
      cmd
        .setNx(k, value.asJson.noSpaces)
        .flatMap { result =>
          Logger[F].info(s"For key $k set result into api cache is: $result.")
        }
    }

    private def mkKey(key: RequestHash32): String =
      s"ergo.explorer.${key.value}"

    def get(key: RequestHash32): F[Option[CachedResponse]] =
      for {
        _ <- Logger[F].info(s"Going to put get $key from api cache.")
        r <- cmd.get(mkKey(key)).map(_.flatMap(parse(_).toOption)).map { jsonOpt =>
               jsonOpt.flatMap(_.as[CachedResponse].toOption)
             }
        _ <- Logger[F].info(s"Get key $key result from api cache is $r.")
      } yield r

  }
}
