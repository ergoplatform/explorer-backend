package org.ergoplatform.explorer.settings

import cats.data.NonEmptyList
import org.ergoplatform.explorer.UrlString

import scala.concurrent.duration.FiniteDuration

final case class Settings(
  chainPollInterval: FiniteDuration,
  utxPoolPollInterval: FiniteDuration,
  masterNodesAddresses: NonEmptyList[UrlString],
  protocol: ProtocolSettings
)
