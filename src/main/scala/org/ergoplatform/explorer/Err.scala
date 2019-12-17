package org.ergoplatform.explorer

import scala.util.control.NoStackTrace

class Err(msg: String) extends Exception(msg) with NoStackTrace {

  override def getMessage: String = msg
}

object Err {

  def apply(msg: String): Err = new Err(msg)

  final case class NotFound(what: String) extends Err(s"$what not found")

  final case class BadInput(details: String) extends Err(s"Bad input: $details")
}
