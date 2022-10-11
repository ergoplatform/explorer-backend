package org.ergoplatform.explorer.http.api.cache

import cats.Show
import cats.effect.Sync
import io.estatico.newtype.macros.newtype
import org.http4s.Request
import tofu.logging.Loggable
import cats.syntax.functor._

import java.security.MessageDigest

object types {

  @newtype
  case class RequestHash32(value: String)

  object RequestHash32 {
    implicit val show: Show[RequestHash32]         = deriving
    implicit val loggable: Loggable[RequestHash32] = Loggable.show

    def apply[F[_]: Sync](request: Request[F]): F[RequestHash32] =
      request.body.compile.toList.map { body =>
        RequestHash32(
          new String(
            MessageDigest
              .getInstance("SHA-256")
              .digest(
                (request.method.toString ++ request.uri.toString ++ body.map(_.toChar).mkString).getBytes
              )
          )
        )
      }
  }
}
