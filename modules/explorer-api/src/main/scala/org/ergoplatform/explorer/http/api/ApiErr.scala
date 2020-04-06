package org.ergoplatform.explorer.http.api

import cats.ApplicativeError
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import org.ergoplatform.explorer.Err.RequestProcessingErr.AddressDecodingFailed
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import sttp.tapir.Schema

import scala.util.control.NoStackTrace

abstract class ApiErr(val status: Int, val reason: String) extends Exception with NoStackTrace

object ApiErr {

  final case class NotFound(what: String) extends ApiErr(404, s"$what not found")

  final case class BadRequest(details: String) extends ApiErr(400, s"Bad input: $details")

  final case class UnknownErr(message: String) extends ApiErr(500, s"Unknown error: $message")

  implicit val encoder: Encoder[ApiErr] = e =>
    Json.obj("status" -> e.status.asJson, "reason" -> e.reason.asJson)

  private val unknownErrorS = implicitly[Schema[UnknownErr]]
  private val notFoundS     = implicitly[Schema[NotFound]]
  private val badInputS     = implicitly[Schema[BadRequest]]

  implicit val schema: Schema[ApiErr] =
    Schema.oneOf[ApiErr, String](_.getMessage, _.toString)(
      "unknownError" -> unknownErrorS,
      "notFound"     -> notFoundS,
      "badInput"     -> badInputS
    )

  implicit def adaptThrowable[F[_]](
    implicit A: ApplicativeError[F, Throwable]
  ): AdaptThrowableEitherT[F, ApiErr] =
    new AdaptThrowableEitherT[F, ApiErr] {

      final def adapter: Throwable => ApiErr = {
        case AddressDecodingFailed(address, _) =>
          ApiErr.BadRequest(s"Failed to decode address '$address'")
        case e =>
          ApiErr.UnknownErr(e.getMessage)
      }
    }
}
