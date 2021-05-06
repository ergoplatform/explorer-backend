package org.ergoplatform.explorer.settings

import scala.concurrent.duration.FiniteDuration

final case class UtxBroadcasterSettings(
  network: NetworkSettings,
  tickInterval: FiniteDuration,
  redis: RedisSettings,
  utxCache: UtxCacheSettings
)

object UtxBroadcasterSettings extends SettingCompanion[UtxBroadcasterSettings]
