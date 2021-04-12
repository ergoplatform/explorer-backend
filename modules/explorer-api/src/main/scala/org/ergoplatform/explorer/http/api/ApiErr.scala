package org.ergoplatform.explorer.http.api

import cats.MonadError
import cats.syntax.applicative._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.circe.Codec
import io.circe.derivation.deriveCodec
import org.ergoplatform.explorer.Err.RequestProcessingErr.AddressDecodingFailed
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import sttp.tapir.{Schema, Validator}

import scala.util.control.NoStackTrace

abstract class ApiErr extends Exception with NoStackTrace {
  val status: Int
  val reason: String
}

object ApiErr {

  final case class NotFound(status: Int, reason: String) extends ApiErr

  final case class BadRequest(status: Int, reason: String) extends ApiErr

  final case class UnknownErr(status: Int, reason: String) extends ApiErr

  implicit val codec404: Codec[NotFound]   = deriveCodec
  implicit val codec400: Codec[BadRequest] = deriveCodec
  implicit val codec500: Codec[UnknownErr] = deriveCodec

  def notFound(what: String): NotFound = NotFound(404, s"Not found $what")

  def badRequest(details: String): BadRequest = BadRequest(400, s"Bad request: $details")

  def unknownErr(message: String): UnknownErr = UnknownErr(500, s"Unknown error: $message")

  implicit val unknownErrorS: Schema[UnknownErr] = Schema.derived[UnknownErr]
  implicit val notFoundS: Schema[NotFound]       = Schema.derived[NotFound]
  implicit val badInputS: Schema[BadRequest]     = Schema.derived[BadRequest]

  implicit val unknownErrorV: Validator[UnknownErr] = Schema.derived[UnknownErr].validator
  implicit val notFoundV: Validator[NotFound]       = Schema.derived[NotFound].validator
  implicit val badInputV: Validator[BadRequest]     = Schema.derived[BadRequest].validator

  implicit val schema: Schema[ApiErr] =
    Schema.oneOfUsingField[ApiErr, String](_.getMessage, _.toString)(
      "unknownError" -> unknownErrorS,
      "notFound"     -> notFoundS,
      "badInput"     -> badInputS
    )

  implicit def adaptThrowable[F[_]: Logger](implicit
    F: MonadError[F, Throwable]
  ): AdaptThrowableEitherT[F, ApiErr] =
    new AdaptThrowableEitherT[F, ApiErr] {

      final def adapter: Throwable => F[ApiErr] = {
        case AddressDecodingFailed(address, _) =>
          (badRequest(s"Failed to decode address '$address'"): ApiErr).pure[F]
        case e =>
          Logger[F].error(s"Unknown error: $e: ${e.getMessage}") as unknownErr(e.getMessage)
      }
    }
}
