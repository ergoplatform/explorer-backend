package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class ErgoTreeHuman(constants: String, script: String)

object ErgoTreeHuman {

  implicit val schema: Schema[ErgoTreeHuman] =
    Schema
      .derived[ErgoTreeHuman]
      .modify(_.constants)(_.description("Constants use in ergo script"))
      .modify(_.script)(_.description("Human readable ergo script"))

  implicit val validator: Validator[ErgoTreeHuman] = schema.validator
}

@derive(encoder, decoder)
final case class ErgoTreeConversionRequest(hashed: String)

object ErgoTreeConversionRequest {

  implicit val schema: Schema[ErgoTreeConversionRequest] =
    Schema
      .derived[ErgoTreeConversionRequest]
      .modify(_.hashed)(_.description("Hashed value of ergo script"))

  implicit val validator: Validator[ErgoTreeConversionRequest] = schema.validator
}