package org.ergoplatform.explorer

import sttp.tapir.Schema

import scala.util.control.NoStackTrace

class Err(msg: String) extends Exception(msg) with NoStackTrace {

  override def getMessage: String = msg
}

object Err {

  def apply(msg: String): Err = new Err(msg)

  abstract class ApiErr(msg: String) extends Err(msg)

  object ApiErr {

    final case class NotFound(what: String) extends ApiErr(s"$what not found")

    final case class BadInput(details: String) extends ApiErr(s"Bad input: $details")

    final case class UnknownErr(msg: String) extends ApiErr(s"Unknown error: $msg")

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

  final case class InconsistentDbData(details: String)
    extends Err(s"Inconsistent blockchain data in db: $details")

  final case class RefinementFailed(details: String)
    extends Err(s"Refinement failed: $details")
}
