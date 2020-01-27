package org.ergoplatform.explorer.http.api

import org.ergoplatform.explorer.Err
import sttp.tapir.{CodecForOptional, CodecFormat, Schema}
import sttp.tapir.json.circe._
import io.circe.generic.auto._

import scala.util.control.NoStackTrace

abstract class ApiErr(val msg: String) extends Exception with NoStackTrace

object ApiErr {

  final case class NotFound(what: String) extends ApiErr(s"$what not found")

  final case class BadInput(details: String) extends ApiErr(s"Bad input: $details")

  final case class UnknownErr(message: String) extends ApiErr(s"Unknown error: $message")

  implicit val codec: CodecForOptional[ApiErr, CodecFormat.Json, _] =
    implicitly[CodecForOptional[ApiErr, CodecFormat.Json, _]]

  private val unknownErrorS = implicitly[Schema[UnknownErr]]
  private val notFoundS     = implicitly[Schema[NotFound]]
  private val badInputS     = implicitly[Schema[BadInput]]

  implicit val schema: Schema[ApiErr] =
    Schema.oneOf[ApiErr, String](_.getMessage, _.toString)(
      "unknownError" -> unknownErrorS,
      "notFound"     -> notFoundS,
      "badInput"     -> badInputS
    )
}
