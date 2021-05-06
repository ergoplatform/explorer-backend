package org.ergoplatform.explorer.settings

import pureconfig.generic.auto._

import scala.concurrent.duration.FiniteDuration

final case class IndexerSettings(
  pollInterval: FiniteDuration,
  writeOrphans: Boolean,
  network: NetworkSettings,
  db: DbSettings,
  protocol: ProtocolSettings
)

object IndexerSettings extends SettingCompanion[IndexerSettings]
