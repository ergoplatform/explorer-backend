package org.ergoplatform.explorer.http.api.cache

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import fs2.{Chunk, Stream}
import org.ergoplatform.explorer.http.api.cache.models.CachedResponse
import org.ergoplatform.explorer.http.api.cache.types.RequestHash32
import org.http4s.{HttpRoutes, Request, Response}
import org.typelevel.vault.Vault
import tofu.syntax.monadic._

import scala.concurrent.duration.DurationInt

object CachingMiddleware {

  def make[F[_]: Sync](
    caching: ApiQueryCache[F]
  ): CachingMiddleware[F] =
    new CachingMiddleware[F](caching)

  final class CachingMiddleware[F[_]: Sync](cache: ApiQueryCache[F]) {

    def middleware(routes: HttpRoutes[F]) = Kleisli { req: Request[F] =>
      OptionT.liftF(RequestHash32(req)).flatMap { key =>
        OptionT(cache.get(key).map { respOpt =>
          respOpt.map(toResponse)
        }).orElse {
          routes(req).flatMap { response =>
            OptionT.liftF(fromResponse(response)).flatMap { value =>
              OptionT.liftF {
                if (response.status.isSuccess) cache.put(key, value).as(response)
                else response.pure
              }
            }
          }
        }
      }
    }

    def toResponse(resp: CachedResponse): Response[F] =
      Response(
        resp.status,
        resp.httpVersion,
        resp.headers,
        Stream.chunk(Chunk.chars(resp.body.toCharArray)).map(_.toByte),
        Vault.empty
      )

    def fromResponse(resp: Response[F]): F[CachedResponse] =
      resp.body.compile.toList.map { body =>
        CachedResponse(
          resp.status,
          resp.httpVersion,
          resp.headers,
          body.map(_.toChar).mkString
        )
      }

  }

}
