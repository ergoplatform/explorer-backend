package org.ergoplatform.explorer.protocol.models

import enumeratum._
import io.circe.KeyDecoder
import org.ergoplatform.explorer.RegisterId

sealed abstract class RegisterSignature(val id: RegisterId) extends EnumEntry

object RegisterSignature extends Enum[RegisterSignature] with CirceEnum[RegisterSignature] {

  case object R0 extends RegisterSignature(RegisterId.R0)
  case object R1 extends RegisterSignature(RegisterId.R1)
  case object R2 extends RegisterSignature(RegisterId.R2)
  case object R3 extends RegisterSignature(RegisterId.R3)
  case object R4 extends RegisterSignature(RegisterId.R4)
  case object R5 extends RegisterSignature(RegisterId.R5)
  case object R6 extends RegisterSignature(RegisterId.R6)
  case object R7 extends RegisterSignature(RegisterId.R7)
  case object R8 extends RegisterSignature(RegisterId.R8)
  case object R9 extends RegisterSignature(RegisterId.R9)

  val values = findValues

  implicit val decoder: KeyDecoder[RegisterSignature] = RegisterSignature.withNameOption
}
