package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class NetworkInfo(
  protocolVersion: String,
  currentSupply: Long,
  hashRate: Long,
  height: Int,
  epoch: Int,
  nextEpochHeight: Int,
  epochParams: EpochParams
)

object NetworkInfo {

  implicit val schema: Schema[NetworkInfo] =
    Schema
      .derived[NetworkInfo]
      .modify(_.protocolVersion)(_.description("Ergo protocol version"))
      .modify(_.currentSupply)(_.description("Current supply of ERGs"))
      .modify(_.hashRate)(_.description("Current hash rate in the network"))
      .modify(_.height)(_.description("Current blockchain height"))
      .modify(_.epoch)(_.description("Current blockchain epoch"))
      .modify(_.nextEpochHeight)(_.description("Next epoch height"))
      .modify(_.epochParams)(_.description("Current epoch params"))

  implicit val validator: Validator[NetworkInfo] =
    schema.validator
}
