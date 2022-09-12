package org.ergoplatform.explorer.http.api.cache

import cats.Show
import cats.effect.Sync
import io.estatico.newtype.macros.newtype
import org.http4s.Request
import tofu.logging.Loggable

object types {

  @newtype
  case class RequestHash32(value: String)

  object RequestHash32 {
    implicit val show: Show[RequestHash32] = deriving
    implicit val loggable: Loggable[RequestHash32] = Loggable.show

    def apply[F[_] : Sync](request: Request[F]): RequestHash32 =
      RequestHash32(
        request.method.toString ++ request.uri.toString ++ request.body.compile.toString
      )
  }
}