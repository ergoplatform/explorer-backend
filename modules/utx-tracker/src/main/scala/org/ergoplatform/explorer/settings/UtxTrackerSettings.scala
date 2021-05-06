package org.ergoplatform.explorer.settings

import scala.concurrent.duration.FiniteDuration

final case class UtxTrackerSettings(
  pollInterval: FiniteDuration,
  network: NetworkSettings,
  db: DbSettings,
  protocol: ProtocolSettings
)

object UtxTrackerSettings extends SettingCompanion[UtxTrackerSettings]
