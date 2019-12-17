package org.ergoplatform.explorer

import scala.util.control.NoStackTrace

case class Exc(msg: String) extends Exception(msg) with NoStackTrace {

  override def getMessage: String = msg
}
