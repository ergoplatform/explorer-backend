package org.ergoplatform.explorer.protocol

import io.circe.Json
import io.circe.syntax._
import org.ergoplatform.explorer.{HexString, RegisterId}
import org.ergoplatform.explorer.protocol.models.{ExpandedRegister, RegisterValue}

import scala.util.Try

object registers {

  /** Expand registers into `register_id -> expanded_register` form.
    */
  @inline def expand(registers: Map[RegisterId, HexString]): Map[RegisterId, ExpandedRegister] = {
    val expanded =
      for {
        (idSig, rawValue)               <- registers.toList
        RegisterValue(valueType, value) <- RegistersParser[Try].parse(rawValue).toOption
      } yield idSig -> ExpandedRegister(rawValue, valueType, value)
    expanded.toMap
  }

  /** Convolve registers into `register_id -> raw_value` form.
    */
  @inline def convolveJson(expanded: Json): Json =
    expanded
      .as[Map[RegisterId, ExpandedRegister]]
      .map(_.mapValues(_.rawValue).asJson)
      .fold(_ => Json.obj(Seq.empty: _*), identity)
}
