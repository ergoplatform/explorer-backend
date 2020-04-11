package org.ergoplatform.explorer.http.api

import cats.MonadError
import cats.syntax.applicative._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.circe.syntax._
import io.circe.{Codec, Decoder, Encoder, Json}
import org.ergoplatform.explorer.Err.RequestProcessingErr.AddressDecodingFailed
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import sttp.tapir.Schema

import scala.util.control.NoStackTrace

final case class ApiErr(status: Int, reason: String) extends Exception with NoStackTrace

object ApiErr {

  def notFound(what: String): ApiErr = ApiErr(404, s"$what not found")

  def badRequest(details: String): ApiErr = ApiErr(400, s"Bad input: $details")

  def unknownErr(message: String): ApiErr = ApiErr(500, s"Unknown error: $message")

  implicit val encoder: Encoder[ApiErr] = e =>
    Json.obj("status" -> e.status.asJson, "reason" -> e.reason.asJson)

  implicit val decoder: Decoder[ApiErr] = Decoder { c =>
    for {
      status <- c.downField("status").as[Int]
      reason <- c.downField("reason").as[String]
    } yield ApiErr(status, reason)
  }
  implicit val codec: Codec[ApiErr] = Codec.from(decoder, encoder)

  implicit val schema: Schema[ApiErr] = implicitly[Schema[ApiErr]]

  implicit def adaptThrowable[F[_]: Logger](
    implicit F: MonadError[F, Throwable]
  ): AdaptThrowableEitherT[F, ApiErr] =
    new AdaptThrowableEitherT[F, ApiErr] {

      final def adapter: Throwable => F[ApiErr] = {
        case AddressDecodingFailed(address, _) =>
          badRequest(s"Failed to decode address '$address'").pure
        case e =>
          Logger[F].error(s"Unknown error: ${e.getMessage}") as unknownErr(e.getMessage)
      }
    }
}
