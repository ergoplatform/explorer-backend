package org.ergoplatform.explorer.settings

import scala.concurrent.duration.FiniteDuration

final case class EnabledIndexes(
  boxRegisters: Boolean,
  scriptConstants: Boolean,
  blockExtensions: Boolean,
  adProofs: Boolean,
  blockStats: Boolean
)

final case class IndexerSettings(
  chainPollInterval: FiniteDuration,
  epochPollInterval: FiniteDuration,
  writeOrphans: Boolean,
  network: NetworkSettings,
  db: DbSettings,
  protocol: ProtocolSettings,
  indexes: EnabledIndexes,
  startHeight: Option[Int],
  redisCache: RedisSettings
)

object IndexerSettings extends SettingCompanion[IndexerSettings]
