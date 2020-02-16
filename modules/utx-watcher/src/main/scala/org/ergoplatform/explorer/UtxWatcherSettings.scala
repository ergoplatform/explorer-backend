package org.ergoplatform.explorer

import org.ergoplatform.explorer.settings.ProtocolSettings

import scala.concurrent.duration.FiniteDuration

final case class UtxWatcherSettings(
                                     pollInterval: FiniteDuration,
                                     protocol: ProtocolSettings
)
