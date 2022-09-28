package org.ergoplatform.explorer.http.api.cache.models

import cats.syntax.either._
import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.{Decoder, Encoder}
import org.http4s.{Header, Headers, HttpVersion, Status}
import org.typelevel.ci.CIString

@derive(encoder, decoder)
case class CachedResponse(
  status: Status,
  httpVersion: HttpVersion,
  headers: Headers,
  body: String
)

object CachedResponse {
  implicit val encoderStatus: Encoder[Status] = Encoder[Int].contramap(_.code)
  implicit val decoderStatus: Decoder[Status] = Decoder[Int].emap(s => Status.fromInt(s).leftMap(_.message))

  implicit val encoderHttpVersion: Encoder[HttpVersion] = Encoder[String].contramap(_.renderString)

  implicit val decoderHttpVersion: Decoder[HttpVersion] =
    Decoder[String].emap(s => HttpVersion.fromString(s).leftMap(_.message))

  implicit val encoderHeaders: Encoder[Headers] = Encoder[List[(String, String)]].contramap { headers =>
    headers.headers.map(h => h.name.toString -> h.value)
  }

  implicit val decoderHeaders: Decoder[Headers] = Decoder[List[(String, String)]].emap { s =>
    Headers.apply(s.map(t => Header.Raw.apply(CIString(t._1), t._2))).asRight
  }
}
